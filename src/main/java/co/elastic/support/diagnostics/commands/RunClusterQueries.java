/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.Constants;
import co.elastic.support.diagnostics.DiagConfig;
import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.rest.RestClient;
import co.elastic.support.rest.RestEntry;
import co.elastic.support.util.JsonYamlUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RunClusterQueries extends BaseQuery {

    /**
     * Builds the list of queries for Elasticsearch version that was retrieved previously,
     * then executes them and saves the result to temporary storage.
     */

    private static final Logger logger = LogManager.getLogger(RunClusterQueries.class);

    private static final String COUNT = "count";
    private static final String CAT_COUNT = "cat_count";
    private static final DateTimeFormatter CAT_TIMESTAMP = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC);

    public void execute(DiagnosticContext context) throws DiagnosticException {

        try {
            DiagConfig diagConfig = context.diagsConfig;
            List<RestEntry> entries = new ArrayList<>();
            entries.addAll(context.elasticRestCalls.values());
            RestClient client;
            client = context.resourceCache.getRestClient(Constants.restInputHost);
/*            if(ResourceCache.resourceExists(Constants.restTargetHost)){
                client = ResourceCache.getRestClient(Constants.restTargetHost);
            }
            else {
                client = ResourceCache.getRestClient(Constants.restInputHost);
            }*/

            runQueries(client, entries, context.tempDir, diagConfig.callRetries, diagConfig.pauseRetries);
            deriveCatCount(context);
        } catch (Throwable t) {
            logger.error( "Error executing REST queries", t);
            throw new DiagnosticException(String.format("Unrecoverable REST Query Execution error - exiting. %s", Constants.CHECK_LOG));
        }

    }

    /**
     * Counting documents is expensive on large clusters, so only /_count is called and
     * cat/cat_count.txt is written from its result, in the same format as /_cat/count,
     * for tools that still read the cat file.
     *
     * @param context The current diagnostic context as set in the DiagnosticService class
     */
    void deriveCatCount(DiagnosticContext context) {
        RestEntry countEntry = context.elasticRestCalls.get(COUNT);
        if (countEntry == null || context.elasticRestCalls.containsKey(CAT_COUNT)) {
            return;
        }

        try {
            Path countFile = Paths.get(context.tempDir, countEntry.getSubdir(), COUNT + countEntry.getExtension());
            if (!Files.isRegularFile(countFile)) {
                logger.warn("{} not found, {} will not be generated.", countFile, CAT_COUNT);
                return;
            }

            JsonNode count = JsonYamlUtils.createJsonNodeFromFileName(countFile.getParent().toString(), countFile.getFileName().toString()).path(COUNT);
            if (!count.isIntegralNumber()) {
                logger.warn("{} does not contain a valid count, {} will not be generated.", countFile, CAT_COUNT);
                return;
            }

            Instant collected = Files.getLastModifiedTime(countFile).toInstant();
            String line = collected.getEpochSecond() + " " + CAT_TIMESTAMP.format(collected) + " " + count.asLong() + "\n";

            Path catDir = Paths.get(context.tempDir, "cat");
            Files.createDirectories(catDir);
            Path catFile = catDir.resolve(CAT_COUNT + ".txt");
            Files.writeString(catFile, line, StandardCharsets.UTF_8);
            logger.info(Constants.CONSOLE, "Results written to: {}", catFile);
        } catch (Exception e) {
            logger.error("Unable to generate {} from {}", CAT_COUNT, COUNT, e);
        }
    }

}
