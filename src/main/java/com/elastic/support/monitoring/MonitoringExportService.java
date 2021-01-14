package com.elastic.support.monitoring;

import com.elastic.support.BaseService;
import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.diagnostics.commands.CheckElasticsearchVersion;
import com.elastic.support.rest.*;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.ResourceUtils;
import com.elastic.support.util.SystemProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitoringExportService implements BaseService {

    private static final String SCROLL_ID = "{ \"scroll_id\" : \"{{scrollId}}\" }";
    private Logger logger = LogManager.getLogger(MonitoringExportService.class);

    MonitoringExportInputs inputs;
    MonitoringExportConfig config;

    public MonitoringExportService(MonitoringExportInputs inputs, MonitoringExportConfig config){
        this.inputs = inputs;
        this.config = config;
    }

    public void exec() {
        String monitoringUri = "";

        try{
            ResourceUtils.restClient = new RestClient(
                            inputs, config);

            config.semver = CheckElasticsearchVersion.getElasticsearchVersion(ResourceUtils.restClient);
            String version = config.semver.getValue();
            RestEntryConfig builder = new RestEntryConfig(version);
            Map restCalls = JsonYamlUtils.readYamlFromClasspath(Constants.MONITORING_REST, true);
            Map<String, RestEntry> versionedRestCalls = builder.buildEntryMap(restCalls);
            monitoringUri = versionedRestCalls.get("monitoring-uri").url;


            if (inputs.listClusters) {
                logger.info(Constants.CONSOLE,  "Diaplaying a list of available clusters.");
                showAvailableClusters(config, ResourceUtils.restClient, monitoringUri);
                return;
            }

            if(inputs.type.equalsIgnoreCase("all") || inputs.type.equalsIgnoreCase("monitoring")){
                if (StringUtils.isEmpty(inputs.clusterId)) {
                    throw new DiagnosticException("missingClusterId");
                }
                validateClusterId(inputs.clusterId, config, ResourceUtils.restClient, monitoringUri);
            }

            runExportQueries(inputs.tempDir, ResourceUtils.restClient, config, inputs, versionedRestCalls);

        } catch (DiagnosticException de) {
            switch (de.getMessage()) {
                case "clusterQueryError":
                    logger.error(Constants.CONSOLE, "The cluster id could not be validated on this monitoring cluster due to retrieval errors.");
                    break;
                case "missingClusterId":
                    logger.error(Constants.CONSOLE, "Cluster id is required. Diaplaying a list of available clusters.");
                    showAvailableClusters(config, ResourceUtils.restClient, monitoringUri);
                    break;
                case "noClusterIdFound":
                    logger.error(Constants.CONSOLE, "Entered cluster id not found. Please enure you have a valid cluster_uuid for the monitored clusters.");
                    showAvailableClusters(config, ResourceUtils.restClient, monitoringUri);
                    break;
                default:
                    logger.info(Constants.CONSOLE,  "Entered cluster id not found - unexpected exception. Please enure you have a valid cluster_uuid for the monitored clusters. Check diagnostics.log for more details.");
                    logger.error( de);
            }
            logger.error(Constants.CONSOLE, "Cannot continue processing. Exiting {}", Constants.CHECK_LOG);
        } catch (Throwable t) {
            logger.error( "Unexpected error occurred", t);
            logger.error(Constants.CONSOLE, "Unexpected error. {}", Constants.CHECK_LOG);
        }
    }

    private void showAvailableClusters(MonitoringExportConfig config, RestClient client, String monitoringUri) {
        List<Map<String, String>> clusters = getMonitoredClusters(config, client, monitoringUri);
        outputAvailableClusters(clusters);
    }

    private void validateClusterId(String clusterId, MonitoringExportConfig config, RestClient client, String monitoringUri) {
        String clusterIdQuery = config.queries.get("cluster_id_check");
        clusterIdQuery = clusterIdQuery.replace("{{clusterId}}", clusterId);

        RestResult restResult = client.execPost(monitoringUri, clusterIdQuery);
        if (restResult.getStatus() != 200) {
            logger.error(Constants.CONSOLE,  "Cluster Id validation failed with status: {}, reason: {}.", restResult.getStatus(), restResult.getReason());
            throw new DiagnosticException("clusterQueryError");
        }

        JsonNode nodeResult = JsonYamlUtils.createJsonNodeFromString(restResult.toString());
        JsonNode hitsNode = nodeResult.path("hits");
        long hitCount = hitsNode.path("total").asLong(0);
        if (hitCount <= 0) {
            throw new DiagnosticException("noClusterIdFound");
        }

    }

    private List<Map<String, String>> getMonitoredClusters(MonitoringExportConfig config, RestClient client, String monitoringUri) {
        String clusterIdQuery = config.queries.get("cluster_ids");

        List<Map<String, String>> clusterIds = new ArrayList<>();
        RestResult restResult = client.execPost(monitoringUri, clusterIdQuery);
        if (restResult.getStatus() != 200) {
            logger.error(Constants.CONSOLE,  "Cluster Id listing failed with status: {}, reason: {}.", restResult.getStatus(), restResult.getReason());
            return clusterIds;
        }

        JsonNode nodeResult = JsonYamlUtils.createJsonNodeFromString(restResult.toString());
        JsonNode hitsNode = nodeResult.path("hits").path("hits");
        if (hitsNode.isArray()) {
            ArrayNode hits = (ArrayNode) hitsNode;
            for (JsonNode hit : hits) {
                Map<String, String> display = new HashMap<>();
                display.put("id", hit.path("_source").path("cluster_uuid").asText());
                display.put("name", hit.path("_source").path("cluster_name").asText());
                String displayName = hit.path("_source").path("cluster_settings").path("cluster").path("metadata").path("display_name").asText();
                if (StringUtils.isEmpty(displayName)) {
                    displayName = "none";
                }
                display.put("display name", displayName);
                clusterIds.add(display);
            }
        }

        return clusterIds;

    }

    private void outputAvailableClusters(List<Map<String, String>> clusters) {
        if (clusters.size() == 0) {
            logger.warn(Constants.CONSOLE,  "No clusters identified. Please check your settings.");
        } else {
            logger.info(Constants.CONSOLE,  "Monitored Clusters:");
            for (Map<String, String> cluster : clusters) {
                logger.info(Constants.CONSOLE,  "name: {}   id: {}   display name: {}", cluster.get("name"), cluster.get("id"), cluster.get("display name"));
            }
        }
    }

    private void runExportQueries(String tempDir, RestClient client, MonitoringExportConfig config, MonitoringExportInputs inputs, Map<String, RestEntry> restCalls) {

        //Get the monitoring stats labels and the general query.
        List<String> statsFields = config.getStatsByType(inputs.type);

        String monitoringScroll = Long.toString(config.monitoringScrollSize);
        String monitoringStartUri = restCalls.get("monitoring-start-scroll-uri").url;
        String metricbeatStartUri = restCalls.get("metricbeat-start-scroll-uri").url;
        String monitoringScrollUri = restCalls.get("monitoring-scroll-uri").url;

        for (String stat : statsFields) {
            String startUri;
            logger.info(Constants.CONSOLE,  "Now extracting {}...", stat);
            String statFile;
            String query;
            if(stat.equalsIgnoreCase("index_stats")){
                query = config.queries.get("index_stats");
                startUri = monitoringStartUri.replace("{{type}}", "es");
                statFile = tempDir + SystemProperties.fileSeparator + stat + ".json";
            }
            else if (config.logstashSets.contains(stat)) {
                query = config.queries.get("general");
                startUri = monitoringStartUri.replace("{{type}}", "logstash");
                statFile = tempDir + SystemProperties.fileSeparator + stat + ".json";
            }
            else if(config.metricSets.contains(stat)){
                query = config.queries.get("metricbeat");
                startUri = metricbeatStartUri;
                statFile = tempDir + SystemProperties.fileSeparator + "metricbeat-" + stat + ".json";
            }
            else{
                query = config.queries.get("general");
                startUri = monitoringStartUri.replace("{{type}}", "es");
                statFile = tempDir + SystemProperties.fileSeparator + stat + ".json";
            }

            query = query.replace("{{type}}", stat);
            query = query.replace("{{size}}", monitoringScroll);
            query = query.replace("{{start}}", inputs.queryStartDate);
            query = query.replace("{{stop}}", inputs.queryEndDate);
            query = query.replace("{{clusterId}}", inputs.clusterId);

            PrintWriter pw = null;
            try {
                RestResult restResult = client.execPost(startUri, query);
                if (restResult.getStatus() != 200) {
                    logger.error(Constants.CONSOLE,  "Initial retrieve for stat: {} failed with status: {}, reason: {}, bypassing and going to next call.", stat, restResult.getStatus(), restResult.getReason());
                    logger.error(Constants.CONSOLE,  "Bypassing.");
                    continue;
                }

                JsonNode resultNode = JsonYamlUtils.createJsonNodeFromString(restResult.toString());
                long totalHits = resultNode.path("hits").path("total").asLong(0);

                // If there are no hits, move to the next.
                if (totalHits > 0) {
                    logger.info(Constants.CONSOLE,  "{} documents retrieved. Writing to disk.", totalHits);
                    //pw = new PrintWriter(new BufferedWriter(new FileWriter(statFile)));
                    Path path = FileSystems.getDefault().getPath(statFile);
                    pw = new PrintWriter(Files.newBufferedWriter(path));
                } else {
                    logger.info(Constants.CONSOLE,  "No documents found for: {}.", stat);
                    continue;
                }

                ArrayNode hitsNode = getHitsArray(resultNode);
                long hitsCount = hitsNode.size();
                long processedHits = 0;
                String scrollId;
                do {
                    // We may have multiple scrolls coming back so process the first one.
                    processHits(hitsNode, pw);
                    processedHits += hitsNode.size();
                    logger.info(Constants.CONSOLE,  "{} of {} processed.", processedHits, totalHits);
                    scrollId = resultNode.path("_scroll_id").asText();
                    String scrollQuery = SCROLL_ID.replace("{{scrollId}}", scrollId);
                    int tries = 1;
                    boolean done = false;
                    while (!done) {
                        try {
                            RestResult scrollResult = client.execPost(monitoringScrollUri, scrollQuery);
                            if (restResult.getStatus() == 200) {
                                resultNode = JsonYamlUtils.createJsonNodeFromString(scrollResult.toString());
                                hitsNode = getHitsArray(resultNode);
                                hitsCount = hitsNode.size();
                            } else {
                                logger.error(Constants.CONSOLE,  "Scroll for stat: {} Operation failed with status: {}, reason: {}, bypassing and going to next call.", stat, restResult.getStatus(), restResult.getReason());
                            }
                            done = true;

                        } catch (Exception e) {
                            logger.error(Constants.CONSOLE,  "Retrieval error, retry {} of 3", tries, e.getMessage());
                            logger.error("Caused by:",  e);
                            if(tries > 3){
                                done = true;
                            }
                            else {
                               tries++;
                            }
                        }
                    }
                } while (hitsCount != 0);

                // Delete the scroll to free up the resources
                client.execDelete("/_search/scroll/" + scrollId);

            } catch (Exception e) {
                logger.error( "Error extracting information from {}", stat, e);
            } finally {
                if (pw != null) {
                    pw.close();
                }
            }
        }
    }

    private ArrayNode getHitsArray(JsonNode resultNode) {
        JsonNode hitsNode = resultNode.path("hits").path("hits");
        if (!hitsNode.isArray()) {
            logger.warn(Constants.CONSOLE,  "Hits array not present-writing empty node.");
            return JsonYamlUtils.mapper.createArrayNode();

        }
        return (ArrayNode) hitsNode;
    }

    private void processHits(ArrayNode hits, PrintWriter pw) throws Exception {
        // We write each hit document out as an individual line to make it easier to
        // bulk index these when they come back in.
        for (JsonNode hit : hits) {
            ((ObjectNode) hit).remove("sort");
            JsonNode src = hit.path("_source");
            pw.println(JsonYamlUtils.mapper.writeValueAsString(src));
        }
    }
}
