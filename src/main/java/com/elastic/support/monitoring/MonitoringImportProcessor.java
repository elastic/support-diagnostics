package com.elastic.support.monitoring;

import com.elastic.support.Constants;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.ArchiveEntryProcessor;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class MonitoringImportProcessor implements ArchiveEntryProcessor {

    private static final Logger logger = LogManager.getLogger(MonitoringImportProcessor.class);

    RestClient client;
    MonitoringExportConfig config;
    MonitoringImportInputs inputs;
    String newClusterName;
    String index;
    boolean updateClusterName = false;

    public MonitoringImportProcessor(MonitoringExportConfig config, MonitoringImportInputs inputs, RestClient client) {
        this.config = config;
        this.inputs = inputs;
        this.client = client;

        // Check for overrides
        if(StringUtils.isNotEmpty(inputs.clusterName)){
            this.newClusterName = inputs.clusterName;
            updateClusterName = true;
        }

        if(StringUtils.isEmpty(inputs.indexName )){
            index = Constants.MONITORING_PREFIX  + SystemProperties.getUtcDateString();
        }
        else{
            index = Constants.MONITORING_PREFIX   + inputs.indexName;
        }

    }

    public void process(InputStream instream, String name) {

        if(name.contains(".log")){
            return;
        }

        logger.info("Processing: {}", name);
        long eventsWritten = 0;

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(instream));
            StringBuilder batchBuilder = new StringBuilder();
            String contents;
            int batch = 0;
            Map<String, Map> inputIndex = new LinkedHashMap();
            Map<String, String> inputIndexField = new LinkedHashMap<>();
            inputIndexField.put("_index", index);
            inputIndex.put("index", inputIndexField);
            String indexLine = JsonYamlUtils.mapper.writeValueAsString(inputIndex);

            try {
                while ( ( contents = br.readLine()) != null){
                    // If clustername is present and they changed it, update
                    ObjectNode sourceObject = JsonYamlUtils.mapper.readValue(contents, ObjectNode.class);
                    String clusterName = sourceObject.path("cluster_name").asText();

                    if(updateClusterName && StringUtils.isNotEmpty(clusterName)){
                        sourceObject.put("clusterName", clusterName);
                    }

                    String sourceLine= JsonYamlUtils.mapper.writeValueAsString(sourceObject);

                    batchBuilder.append(indexLine + "\n");
                    batchBuilder.append(sourceLine + "\n");

                    // See if we need to
                    if(batch >= config.bulkSize){
                        logger.info("Indexing document batch {} to {}",  eventsWritten, eventsWritten+batch);

                        long docsWritten = writeBatch(batchBuilder.toString(), batch);
                        eventsWritten += docsWritten;
                        batch = 0;
                        batchBuilder.setLength(0);
                    }
                    else {
                        batch++;
                    }
                }

                // if there's anything left do the cleanup
                if(batch > 0){
                    logger.info("Indexing document batch {} to {}",  eventsWritten, eventsWritten+batch);
                    long docsWritten = writeBatch(batchBuilder.toString(), batch);
                    eventsWritten  += docsWritten;
                }

            } catch (Throwable t) {
                // If something goes wrong just log it and keep boing.
                logger.error("Error processing JSON event for {}.", t);
            }
        } catch (Throwable t) {
            logger.error("Error processing entry - stream related error,", t);
        }
        finally {
            logger.info("{} events written from {}", eventsWritten, name);
        }

    }

    private long writeBatch(String query, int size){
        RestResult res = new RestResult( client.execPost("_bulk", query), "_bulk");
        if( res.getStatus() != 200){
            logger.error("Batch update had errors: {}  {}", res.getStatus(), res.getReason());
            logger.error(Constants.CHECK_LOG);
            logger.log(SystemProperties.DIAG, res.toString());
            return 0;
        }
        else {
            return size;
        }
    }

}
