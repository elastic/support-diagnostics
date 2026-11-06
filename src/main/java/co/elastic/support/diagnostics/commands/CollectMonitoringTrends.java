/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.Constants;
import co.elastic.support.diagnostics.chain.Command;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.rest.RestClient;
import co.elastic.support.rest.RestResult;
import co.elastic.support.util.JsonYamlUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class CollectMonitoringTrends implements Command {

    private static final Logger logger = LogManager.getLogger(CollectMonitoringTrends.class);

    private static final String AGG_QUERY = "{"
        + "\"size\": 0,"
        + "\"timeout\": \"10s\","
	+ "\"query\": { \"bool\": { \"filter\": ["
        + "  { \"term\": { \"type\": \"node_stats\" } },"
        + "  { \"range\": { \"timestamp\": { \"gte\": \"now-7d\" } } }"
        + "] } },"
        + "\"aggs\": {"
        + "  \"by_node\": {"
        + "    \"terms\": { \"field\": \"source_node.name\", \"size\": 50 },"
        + "    \"aggs\": {"
        + "      \"cpu_pct\": { \"percentiles\": { \"field\": \"node_stats.process.cpu.percent\", \"percents\": [50, 95, 99] } },"
        + "      \"heap_pct\": { \"percentiles\": { \"field\": \"node_stats.jvm.mem.heap_used_percent\", \"percents\": [50, 95, 99] } },"
        + "      \"by_day\": {"
        + "        \"date_histogram\": { \"field\": \"timestamp\", \"fixed_interval\": \"1d\" },"
        + "        \"aggs\": {"
        + "          \"cpu_pct\": { \"percentiles\": { \"field\": \"node_stats.process.cpu.percent\", \"percents\": [50, 95, 99] } },"
        + "          \"heap_pct\": { \"percentiles\": { \"field\": \"node_stats.jvm.mem.heap_used_percent\", \"percents\": [50, 95, 99] } }"
        + "        }"
        + "      }"
        + "    }"
        + "  }"
        + "}"
        + "}";

    public void execute(DiagnosticContext context) {
        if (!context.diagnosticInputs.includeTrends) {
            return;
        }

        try {
            RestClient client = context.resourceCache.getRestClient(Constants.restInputHost);

            // 1) Check whether monitoring indices exist at all - skip quietly if not.
            RestResult checkResult = client.execQuery("/.monitoring-es-*/_search?size=0");
            JsonNode checkNode = JsonYamlUtils.createJsonNodeFromString(checkResult.toString());
            long totalShards = checkNode.path("_shards").path("total").asLong(0);

            if (totalShards == 0) {
                logger.info(Constants.CONSOLE, "No monitoring indices found - skipping trend summary.");
                return;
            }

            // 2) Run the aggregation query.
            HttpResponse response = client.execPost("/.monitoring-es-*/_search", AGG_QUERY);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String body = entity != null ? EntityUtils.toString(entity) : "";

            if (status < 200 || status >= 300) {
                logger.info(Constants.CONSOLE, "Monitoring trend query failed (status {}) - skipping.", status);
                return;
            }

            // 3) Write the result to the diagnostic output directory.
            File outFile = new File(context.tempDir, "monitoring-trends.json");
            FileUtils.writeStringToFile(outFile, body, "UTF-8");
            logger.info(Constants.CONSOLE, "Monitoring trend summary written to: {}", outFile.getName());

        } catch (Exception e) {
            // This feature failing should never block the rest of the diagnostic.
            logger.info(Constants.CONSOLE, "Could not collect monitoring trend summary - bypassing.");
            logger.error("Error collecting monitoring trends", e);
        }
    }
}
