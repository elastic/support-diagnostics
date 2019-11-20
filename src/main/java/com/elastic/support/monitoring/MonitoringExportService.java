package com.elastic.support.monitoring;

import com.elastic.support.ElasticClientService;
import com.elastic.support.config.Constants;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitoringExportService extends ElasticClientService {

    private Logger logger = LogManager.getLogger(MonitoringExportService.class);
    private static final String SCROLL_ID = "{ \"scroll_id\" : \"{{scrollId}}\" }";
    private RestClient client;
    private MonitoringExportConfig config;
    private MonitoringExportInputs inputs;
    private String tempDir = SystemProperties.userDir + SystemProperties.fileSeparator + Constants.MONITORING_DIR;

    public MonitoringExportService(MonitoringExportInputs inputs) {
        this.inputs =  inputs;
        Map configMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
        config = new MonitoringExportConfig(configMap);
        client = createEsRestClient(config, inputs);
        if(StringUtils.isNotEmpty(inputs.getOutputDir())){
            tempDir = inputs.getOutputDir() + SystemProperties.fileSeparator + Constants.MONITORING_DIR;;
        }
    }

    public void execExtract() {

        try {
            // Create the temp directory - delete if first if it exists from a previous run
            logger.info("Creating temp directory: {}", tempDir);

            FileUtils.deleteDirectory(new File(tempDir));
            Files.createDirectories(Paths.get(tempDir));

            logger.info("Sucessfully created temp directory.");

            // Set up the log file manually since we're going to package it with the diagnostic.
            // It will go to wherever we have the temp dir set up.
            logger.info("Configuring log file.");
            createFileAppender(tempDir, "extract.log");

            // They may just want a list of available clusters
            if(inputs.listClusters){
                List<Map<String, String>> clusters = getMonitoredClusters();
                displayAvailableClusters(clusters);
            }
            else{
                // Run service logic here
                validateClusterId();
                runExportQueries();
                closeLogs();
                createArchive(tempDir);
            }
            SystemUtils.nukeDirectory(tempDir);
        }catch (DiagnosticException de){
            if(de.getMessage().equalsIgnoreCase("clusterQueryError")){
                logger.error("The cluster id could not be validated on this monitoring cluster due to errors. Stopping.");
            }
            else if(de.getMessage().equalsIgnoreCase("noClusterIdFound")){
                logger.error("Entered cluster id not found. Please enure you have a valid cluster_uuid for the monitored clusters.");
                logger.info("Listing available monitored clusters on this deployment:");
                List<Map<String, String>> clusters = getMonitoredClusters();
                displayAvailableClusters(clusters);
            }
            logger.error("Cannot contiue processing. Exiting {}", Constants.CHECK_LOG);

        } catch (IOException e) {
            logger.error("Access issue with temp directory", e);
            throw new RuntimeException("Issue with creating temp directory - see logs for details.");
        } catch(Throwable t){
            logger.log(SystemProperties.DIAG, "Unexpected error occurred", t);
            logger.error("Unexpected error. {}", Constants.CHECK_LOG);
        }

        finally {
            closeLogs();
            client.close();
        }
    }

    private void validateClusterId(){

        if(StringUtils.isEmpty(inputs.clusterId)){
            logger.error("Cluster id is required. Diaplaying a list of available clusters.");
            List<Map<String, String>> clusters = getMonitoredClusters();
            displayAvailableClusters(clusters);
            throw new DiagnosticException("missingClusterId");
        }

        String clusterIdQuery = config.queries.get("cluster_id_check");
        clusterIdQuery = clusterIdQuery.replace("{{clusterId}}", inputs.clusterId);

        RestResult restResult = new RestResult(client.execPost(config.monitoringUri , clusterIdQuery), config.monitoringUri);
        if (restResult.getStatus() != 200) {
            logger.info("Cluster Id validation failed with status: {}, reason: {}.", restResult.getStatus(), restResult.getReason());
            throw new DiagnosticException("clusterQueryError");
        }

        JsonNode nodeResult = JsonYamlUtils.createJsonNodeFromString(restResult.toString());
        JsonNode hitsNode = nodeResult.path("hits");
        long hitCount = hitsNode.path("total").asLong(0);
        if (hitCount == 0){
            throw new DiagnosticException("noClusterIdFound");
        }

    }

    private List<Map<String, String>> getMonitoredClusters(){

        String clusterIdQuery = config.queries.get("cluster_ids");

        List<Map<String, String>> clusterIds = new ArrayList<>();
        RestResult restResult = new RestResult(client.execPost(config.monitoringUri , clusterIdQuery), config.monitoringUri);
        if (restResult.getStatus() != 200) {
            logger.error("Cluster Id listing failed with status: {}, reason: {}.", restResult.getStatus(), restResult.getReason());
            return clusterIds;
        }

        JsonNode nodeResult = JsonYamlUtils.createJsonNodeFromString(restResult.toString());
        JsonNode hitsNode = nodeResult.path("hits").path("hits");
        if(hitsNode.isArray()){
            ArrayNode hits = (ArrayNode)hitsNode;
            for(JsonNode hit: hits){
                Map<String, String> display = new HashMap<>();
                display.put("id", hit.path("_source").path("cluster_uuid").asText());
                display.put("name", hit.path("_source").path("cluster_name").asText());
                String displayName = hit.path("_source").path("cluster_settings").path("cluster").path("metadata").path("display_name").asText();
                if(StringUtils.isEmpty(displayName)){
                    displayName = "none";
                }
                display.put("display name", displayName);
                clusterIds.add(display);
            }
        }

        return clusterIds;

    }

    private void displayAvailableClusters(List<Map<String, String>> clusters){
        if(clusters.size() == 0 ){
            logger.info("No clusters identified. Please check your settings.");
        }
        else{
            logger.info("Monitored Clusters:");
            for(Map<String, String> cluster: clusters){
                logger.info("name: {}   id: {}   display name: {}", cluster.get("name"), cluster.get("id"), cluster.get("display name"));
            }
        }
    }

    private void runExportQueries() {

        //Get the monitoring stats labels and the general query.
        List<String> statsFields = config.monitoringStats;

        String uri = config.monitoringUri + config.monitoringScrollTtl;
        String scrollUri = config.monitoringScrollUri + config.monitoringScrollTtl;
        String monitoringScroll = Long.toString(config.monitoringScrollSize);
        String general = config.queries.get("general");
        String indexStats = config.queries.get("index_stats");

        for (String stat : statsFields) {
            logger.info("Now extracting {}...", stat);
            String statFile = tempDir + SystemProperties.fileSeparator + stat + ".json";
            String query;
            if(stat.equals("index_stats")){
                query = indexStats;
            }
            else{
                query = general;
            }

            query = query.replace("{{type}}", stat);
            query = query.replace("{{size}}", monitoringScroll);
            query = query.replace("{{start}}", inputs.queryStartDate);
            query = query.replace("{{stop}}", inputs.queryEndDate);
            query = query.replace("{{clusterId}}", inputs.clusterId);


            PrintWriter pw = null;
            try {

                String scrollId = "";

                RestResult restResult = new RestResult(client.execPost(uri, query), uri);
                if (restResult.getStatus() != 200) {
                    logger.error("Initial retrieve for stat: {} failed with status: {}, reason: {}, bypassing and going to next call.", stat, restResult.getStatus(), restResult.getReason());
                    logger.error("Bypassing.");
                    continue;
                }

                JsonNode resultNode = JsonYamlUtils.createJsonNodeFromString(restResult.toString());
                long totalHits = resultNode.path("hits").path("total").asLong(0);

                // If there are no hits, move to the next.
                if (totalHits > 0) {
                    logger.info("{} documents retrieved. Writing to disk.", totalHits);
                    pw = new PrintWriter(statFile);
                } else {
                    logger.info("No documents found for: {}.", stat);
                    continue;
                }

                ArrayNode hitsNode = getHitsArray(resultNode);
                long hitsCount = hitsNode.size();
                long processedHits = 0;

                do {
                    // We may have multiple scrolls coming back so process the first one.
                    processHits(hitsNode, pw);
                    processedHits += hitsNode.size();
                    logger.info("{} of {} processed.", processedHits, totalHits);

                    scrollId = resultNode.path("_scroll_id").asText();
                    String scrollQuery = SCROLL_ID.replace("{{scrollId}}", scrollId);
                    RestResult scrollResult = new RestResult(client.execPost(scrollUri, scrollQuery), scrollUri);
                    if (restResult.getStatus() == 200) {
                        resultNode = JsonYamlUtils.createJsonNodeFromString(scrollResult.toString());
                        hitsNode = getHitsArray(resultNode);
                        hitsCount = hitsNode.size();
                    } else {
                        logger.error("Scroll for stat: {} Operation failed with status: {}, reason: {}, bypassing and going to next call.", stat, restResult.getStatus(), restResult.getReason());
                    }

                } while (hitsCount != 0);

                // Delete the scroll to free up the resources
                client.execDelete("/_search/scroll/" + scrollId);

            }
            catch (Exception e){
                logger.log(SystemProperties.DIAG, "Error extracting information from {}", stat, e);
            }
            finally {
                if(pw != null) {
                    pw.close();
                }
            }
        }
    }

    private ArrayNode getHitsArray(JsonNode resultNode){
        JsonNode hitsNode = resultNode.path("hits").path("hits");
        if(! hitsNode.isArray()){
            logger.warn("Hits array not present-writing empty node.");
            return JsonYamlUtils.mapper.createArrayNode();

        }
        return  (ArrayNode)hitsNode;
    }

    private void processHits(ArrayNode hits, PrintWriter pw) throws Exception{
        // We write each hit document out as an individual line to make it easier to
        // bulk index these when they come back in.
        for(JsonNode hit: hits){
            ((ObjectNode) hit).remove("sort");
            JsonNode src = hit.path("_source");
            pw.println(JsonYamlUtils.mapper.writeValueAsString(src));
        }
    }
}
