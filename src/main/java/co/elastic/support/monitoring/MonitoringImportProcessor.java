/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.monitoring;

import co.elastic.support.Constants;
import co.elastic.support.rest.RestClient;
import co.elastic.support.rest.RestResult;
import co.elastic.support.util.JsonYamlUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

public class MonitoringImportProcessor {

    private static final Logger logger = LogManager.getLogger(MonitoringImportProcessor.class);

    RestClient client;
    MonitoringImportConfig config;
    MonitoringImportInputs inputs;
    String newClusterName;
    boolean updateClusterName = false;

    public MonitoringImportProcessor(MonitoringImportConfig config, MonitoringImportInputs inputs, RestClient client) {
        this.config = config;
        this.inputs = inputs;
        this.client = client;

        // Check for overrides
        if (StringUtils.isNotEmpty(inputs.clusterName)) {
            this.newClusterName = inputs.clusterName;
            updateClusterName = true;
        }

        checkForExtractTemplates();
    }

    public void exec(Vector<File> files) {

        try {
            for (File file : files) {
                if (file.isDirectory() || file.getName().contains(".log")) {
                    continue;
                }
                logger.info("Processing: {}", file.getName());
                process(file);
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public void process(File file) {

        logger.info(Constants.CONSOLE, "Processing: {}", file.getName());
        long eventsWritten = 0;
        String indexName;

        try (InputStream instream = new FileInputStream(file)) {

            if (file.getName().contains("logstash")) {
                indexName = config.logstashExtractIndexPattern.replace("{{suffix}}", inputs.targetSuffix);
            } else if (file.getName().contains("metricbeat")) {
                indexName = config.metricbeatExtractIndexPattern.replace("{{suffix}}", inputs.targetSuffix);
            } else {
                indexName = config.monitoringExtractIndexPattern.replace("{{suffix}}", inputs.targetSuffix);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(instream));
            StringBuilder batchBuilder = new StringBuilder();
            String contents;
            int batch = 0;
            Map<String, String> inputMeta = new LinkedHashMap<>();
            Map<String, Map> inputAction = new LinkedHashMap();

            try {
                // Pre-fetching to determine source version
                contents = br.readLine();
                ObjectNode sourceObject = JsonYamlUtils.mapper.readValue(contents, ObjectNode.class);
                if (StringUtils.isEmpty(sourceObject.path("type").asText())) {
                    indexName = indexName.replace("{{version}}", "8");
                    logger.info(Constants.CONSOLE, "Targeting {} because no type field is found", indexName);
                    inputMeta.put("_index", indexName);
                    inputAction.put("create", inputMeta);
                } else {
                    indexName = indexName.replace("{{version}}", "7");
                    logger.info(Constants.CONSOLE, "Targeting {} because type field is found", indexName);
                    inputMeta.put("_index", indexName);
                    inputAction.put("index", inputMeta);
                }

                String actionLine = JsonYamlUtils.mapper.writeValueAsString(inputAction);

                while (contents != null) {
                    // If clustername is present and they changed it, update
                    sourceObject = JsonYamlUtils.mapper.readValue(contents, ObjectNode.class);
                    String clusterName = sourceObject.path("cluster_name").asText();

                    if (updateClusterName && StringUtils.isNotEmpty(clusterName)) {
                        sourceObject.put("cluster_name", newClusterName);
                    }

                    String altClusterName = sourceObject.path("cluster_settings").path("cluster").path("metadata").path("display_name").asText();
                    if (updateClusterName && StringUtils.isNotEmpty(altClusterName)) {
                        sourceObject.with("cluster_settings").with("cluster").with("metadata").put("display_name", newClusterName);
                    }

                    String sourceLine = JsonYamlUtils.mapper.writeValueAsString(sourceObject);

                    batchBuilder.append(actionLine + "\n");
                    batchBuilder.append(sourceLine + "\n");

                    // See if we need to
                    if (batch >= config.bulkSize) {
                        logger.info(Constants.CONSOLE, "Indexing document batch {} to {}", eventsWritten, eventsWritten + batch);

                        long docsWritten = writeBatch(batchBuilder.toString(), batch);
                        eventsWritten += docsWritten;
                        batch = 0;
                        batchBuilder.setLength(0);
                    } else {
                        batch++;
                    }
                    contents = br.readLine();
                }

                // if there's anything left do the cleanup
                if (batch > 0) {
                    logger.info(Constants.CONSOLE, "Indexing document batch {} to {}", eventsWritten, eventsWritten + batch);
                    long docsWritten = writeBatch(batchBuilder.toString(), batch);
                    eventsWritten += docsWritten;
                }

            } catch (Throwable t) {
                // If something goes wrong just log it and keep boing.
                logger.error(Constants.CONSOLE, "Error processing JSON event for {}.", t);
            }
        } catch (Throwable t) {
            logger.error(Constants.CONSOLE, "Error processing entry - stream related error,", t);
        } finally {
            logger.info(Constants.CONSOLE, "{} events written from {}", eventsWritten, file.getName());
        }

    }

    public void init(ZipFile zipFile) {
        // Nothing to do here;;
    }

    private long writeBatch(String query, int size) {
        RestResult res = new RestResult(client.execPost("/_bulk", query), "/_bulk");
        if (res.getStatus() != 200) {
            logger.error(Constants.CONSOLE, "Batch update had errors: {}  {}", res.getStatus(), res.getReason());
            logger.error(Constants.CONSOLE, Constants.CHECK_LOG);
            logger.error(res.toString());
            return 0;
        } else {
            return size;
        }
    }

    private void checkForExtractTemplates() {
        for (String template : config.templateList) {
            try {
                if (templateExists(template)) {
                    continue;
                }
                ClassLoader classLoader = getClass().getClassLoader();
                String path = Constants.TEMPLATE_CONFIG_PACKAGE + template + ".json";
                File file = new File(classLoader.getResource(path).getFile());
                String data = FileUtils.readFileToString(file, "UTF-8");
                client.execPost("/_template/" + template, data);
            } catch (Exception e) {
                logger.error("Issue checking template {}", template, e);
            }
        }
    }

    private boolean templateExists(String template) {
        RestResult result = client.execQuery("/_template/" + template);
        if (result.getStatus() != 200) {
            return false;
        }
        return true;
    }

}
