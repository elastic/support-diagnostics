/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.monitoring;

import co.elastic.support.diagnostics.commands.CheckElasticsearchVersion;
import co.elastic.support.Constants;
import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.rest.ElasticRestClientService;
import co.elastic.support.rest.RestClient;
import co.elastic.support.rest.RestEntry;
import co.elastic.support.rest.RestEntryConfig;
import co.elastic.support.rest.RestResult;
import co.elastic.support.util.JsonYamlUtils;
import co.elastic.support.util.SystemProperties;
import co.elastic.support.util.SystemUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitoringExportService extends ElasticRestClientService {

    private static final String SCROLL_ID = "{ \"scroll_id\" : \"{{scrollId}}\" }";
    private Logger logger = LogManager.getLogger(MonitoringExportService.class);

    public void execExtract(MonitoringExportInputs inputs) throws DiagnosticException {
        // Initialize outside the block for Exception handling
        RestClient client = null;
        MonitoringExportConfig config = null;
        String tempDir = SystemProperties.fileSeparator + Constants.MONITORING_DIR;
        String monitoringUri = "";

        try {
            if (StringUtils.isEmpty(inputs.outputDir)) {
                tempDir = SystemProperties.userDir + tempDir;
            } else {
                tempDir = inputs.outputDir + tempDir;
            }

            // Initialize the temp directory first.
            // Set up the log file manually since we're going to package it with the
            // diagnostic.
            // It will go to wherever we have the temp dir set up.
            SystemUtils.nukeDirectory(tempDir);
            Files.createDirectories(Paths.get(tempDir));
            createFileAppender(tempDir, "extract.log");

            Map configMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
            config = new MonitoringExportConfig(configMap);
            client = RestClient.getClient(
                    inputs.host,
                    inputs.port,
                    inputs.scheme,
                    inputs.user,
                    inputs.password,
                    inputs.proxyHost,
                    inputs.proxyPort,
                    inputs.proxyUser,
                    inputs.proxyPassword,
                    inputs.pkiKeystore,
                    inputs.pkiKeystorePass,
                    inputs.skipVerification,
                    config.extraHeaders,
                    config.connectionTimeout,
                    config.connectionRequestTimeout,
                    config.socketTimeout);

            config.semver = CheckElasticsearchVersion.getElasticsearchVersion(client);
            String version = config.semver.getVersion();
            RestEntryConfig builder = new RestEntryConfig(version);
            Map restCalls = JsonYamlUtils.readYamlFromClasspath(Constants.MONITORING_REST, true);
            Map<String, RestEntry> versionedRestCalls = builder.buildEntryMap(restCalls);
            monitoringUri = versionedRestCalls.get("monitoring-uri").url;

            if (inputs.listClusters) {
                logger.info(Constants.CONSOLE, "Displaying a list of available clusters.");
                showAvailableClusters(config, client, monitoringUri);
                return;
            }

            if (inputs.type.equalsIgnoreCase("all") || inputs.type.equalsIgnoreCase("monitoring")) {
                if (StringUtils.isEmpty(inputs.clusterId)) {
                    throw new DiagnosticException("missingClusterId");
                }
                validateClusterId(inputs.clusterId, config, client, monitoringUri);
            }

            runExportQueries(tempDir, client, config, inputs, versionedRestCalls);

        } catch (DiagnosticException de) {
            switch (de.getMessage()) {
                case "clusterQueryError":
                    logger.error(Constants.CONSOLE,
                            "The cluster id could not be validated on this monitoring cluster due to retrieval errors.");
                    break;
                case "missingClusterId":
                    logger.error(Constants.CONSOLE, "Cluster id is required. Diaplaying a list of available clusters.");
                    showAvailableClusters(config, client, monitoringUri);
                    break;
                case "noClusterIdFound":
                    logger.error(Constants.CONSOLE,
                            "Entered cluster id not found. Please enure you have a valid cluster_uuid for the monitored clusters.");
                    showAvailableClusters(config, client, monitoringUri);
                    break;
                default:
                    logger.info(Constants.CONSOLE,
                            "Entered cluster id not found - unexpected exception. Please enure you have a valid cluster_uuid for the monitored clusters. Check diagnostics.log for more details.");
                    logger.error(de);
            }
            logger.error(Constants.CONSOLE, "Cannot continue processing. Exiting {}", Constants.CHECK_LOG);
        } catch (IOException e) {
            logger.error(Constants.CONSOLE, "Access issue with temp directory", e);
            throw new RuntimeException("Issue with creating temp directory - see logs for details.");
        } catch (Throwable t) {
            logger.error("Unexpected error occurred", t);
            logger.error(Constants.CONSOLE, "Unexpected error. {}", Constants.CHECK_LOG);
        } finally {
            closeLogs();
            createArchive(tempDir);
            client.close();
            SystemUtils.nukeDirectory(tempDir);
        }
    }

    private void showAvailableClusters(MonitoringExportConfig config, RestClient client, String monitoringUri) {
        List<Map<String, String>> clusters = getMonitoredClusters(config, client, monitoringUri);
        outputAvailableClusters(clusters);
    }

    private void validateClusterId(String clusterId, MonitoringExportConfig config, RestClient client,
            String monitoringUri) throws DiagnosticException {
        String clusterIdQuery = config.queries.get("cluster_id_check");
        clusterIdQuery = clusterIdQuery.replace("{{clusterId}}", clusterId);

        RestResult restResult = new RestResult(client.execPost(monitoringUri, clusterIdQuery), monitoringUri);
        if (restResult.getStatus() != 200) {
            logger.error(Constants.CONSOLE, "Cluster Id validation failed with status: {}, reason: {}.",
                    restResult.getStatus(), restResult.getReason());
            throw new DiagnosticException("clusterQueryError");
        }

        JsonNode nodeResult = JsonYamlUtils.createJsonNodeFromString(restResult.toString());
        JsonNode hitsNode = nodeResult.path("hits");
        long hitCount = hitsNode.path("total").asLong(0);
        if (hitCount <= 0) {
            throw new DiagnosticException("noClusterIdFound");
        }

    }

    private List<Map<String, String>> getMonitoredClusters(MonitoringExportConfig config, RestClient client,
            String monitoringUri) {
        String clusterIdQuery = config.queries.get("cluster_ids");

        List<Map<String, String>> clusterIds = new ArrayList<>();
        RestResult restResult = new RestResult(client.execPost(monitoringUri, clusterIdQuery), monitoringUri);
        if (restResult.getStatus() != 200) {
            logger.error(Constants.CONSOLE, "Cluster Id listing failed with status: {}, reason: {}.",
                    restResult.getStatus(), restResult.getReason());
            return clusterIds;
        }

        JsonNode nodeResult = JsonYamlUtils.createJsonNodeFromString(restResult.toString());
        JsonNode hitsNode = nodeResult.path("hits").path("hits");
        if (hitsNode.isArray()) {
            ArrayNode hits = (ArrayNode) hitsNode;
            for (JsonNode hit : hits) {
                Map<String, String> display = new HashMap<>();

                String clusterUuid = hit.path("_source").path("cluster_uuid").asText();
                if (StringUtils.isEmpty(clusterUuid)) {
                    clusterUuid = hit.path("fields").path("cluster_uuid").path(0).asText();
                }
                String clusterName = hit.path("_source").path("cluster_name").asText();
                if (StringUtils.isEmpty(clusterName)) {
                    clusterName = hit.path("_source").path("elasticsearch").path("cluster").path("name").asText();
                }
                String displayName = hit.path("_source").path("cluster_settings").path("cluster").path("metadata")
                        .path("display_name").asText();
                if (StringUtils.isEmpty(displayName)) {
                    displayName = "none";
                }

                display.put("id", clusterUuid);
                display.put("name", clusterName);
                display.put("display name", displayName);
                clusterIds.add(display);
            }
        }

        return clusterIds;

    }

    private void outputAvailableClusters(List<Map<String, String>> clusters) {
        if (clusters.size() == 0) {
            logger.warn(Constants.CONSOLE, "No clusters identified. Please check your settings.");
        } else {
            logger.info(Constants.CONSOLE, "Monitored Clusters:");
            for (Map<String, String> cluster : clusters) {
                logger.info(Constants.CONSOLE, "name: {}   id: {}   display name: {}", cluster.get("name"),
                        cluster.get("id"), cluster.get("display name"));
            }
        }
    }

    private void runExportQueries(String tempDir, RestClient client, MonitoringExportConfig config,
            MonitoringExportInputs inputs, Map<String, RestEntry> restCalls) {

        // Get the monitoring stats labels and the general query.
        List<String> statsFields = config.getStatsByType(inputs.type);

        String monitoringScroll = Long.toString(config.monitoringScrollSize);
        String monitoringStartUri = restCalls.get("monitoring-start-scroll-uri").url;
        String metricbeatStartUri = restCalls.get("metricbeat-start-scroll-uri").url;
        String monitoringScrollUri = restCalls.get("monitoring-scroll-uri").url;

        for (String stat : statsFields) {
            String startUri;
            logger.info(Constants.CONSOLE, "Now extracting {}...", stat);
            String statFile;
            String query;
            if (stat.equalsIgnoreCase("index_stats")) {
                query = config.queries.get("index_stats");
                startUri = monitoringStartUri.replace("{{type}}", "es");
                statFile = tempDir + SystemProperties.fileSeparator + stat + ".json";
            } else if (config.logstashSets.contains(stat)) {
                query = config.queries.get("general");
                startUri = monitoringStartUri.replace("{{type}}", "logstash");
                statFile = tempDir + SystemProperties.fileSeparator + stat + ".json";
            } else if (config.metricSets.contains(stat)) {
                query = config.queries.get("metricbeat");
                startUri = metricbeatStartUri;
                statFile = tempDir + SystemProperties.fileSeparator + "metricbeat-" + stat + ".json";
            } else {
                query = config.queries.get("general");
                startUri = monitoringStartUri.replace("{{type}}", "es");
                statFile = tempDir + SystemProperties.fileSeparator + stat + ".json";
            }
            String field = stat.equalsIgnoreCase("shards") ? "shard" : stat;

            query = query.replace("{{type}}", stat);
            query = query.replace("{{field}}", field);
            query = query.replace("{{size}}", monitoringScroll);
            query = query.replace("{{start}}", inputs.queryStartDate);
            query = query.replace("{{stop}}", inputs.queryEndDate);
            query = query.replace("{{clusterId}}", inputs.clusterId);

            PrintWriter pw = null;
            try {
                RestResult restResult = new RestResult(client.execPost(startUri, query), startUri);
                if (restResult.getStatus() != 200) {
                    logger.error(Constants.CONSOLE,
                            "Initial retrieve for stat: {} failed with status: {}, reason: {}, bypassing and going to next call.",
                            stat, restResult.getStatus(), restResult.getReason());
                    logger.error(Constants.CONSOLE, "Bypassing.");
                    continue;
                }

                JsonNode resultNode = JsonYamlUtils.createJsonNodeFromString(restResult.toString());
                long totalHits = resultNode.path("hits").path("total").asLong(0);

                // If there are no hits, move to the next.
                if (totalHits > 0) {
                    logger.info(Constants.CONSOLE, "{} documents retrieved. Writing to disk.", totalHits);
                    pw = new PrintWriter(statFile);
                } else {
                    logger.info(Constants.CONSOLE, "No documents found for: {}.", stat);
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
                    logger.info(Constants.CONSOLE, "{} of {} processed.", processedHits, totalHits);

                    scrollId = resultNode.path("_scroll_id").asText();
                    String scrollQuery = SCROLL_ID.replace("{{scrollId}}", scrollId);
                    RestResult scrollResult = new RestResult(client.execPost(monitoringScrollUri, scrollQuery),
                            monitoringScrollUri);
                    if (restResult.getStatus() == 200) {
                        resultNode = JsonYamlUtils.createJsonNodeFromString(scrollResult.toString());
                        hitsNode = getHitsArray(resultNode);
                        hitsCount = hitsNode.size();
                    } else {
                        logger.error(Constants.CONSOLE,
                                "Scroll for stat: {} Operation failed with status: {}, reason: {}, bypassing and going to next call.",
                                stat, restResult.getStatus(), restResult.getReason());
                    }

                } while (hitsCount != 0);

                // Delete the scroll to free up the resources
                client.execDelete("/_search/scroll/" + scrollId);

            } catch (Exception e) {
                logger.error("Error extracting information from {}", stat, e);
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
            logger.warn(Constants.CONSOLE, "Hits array not present-writing empty node.");
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
