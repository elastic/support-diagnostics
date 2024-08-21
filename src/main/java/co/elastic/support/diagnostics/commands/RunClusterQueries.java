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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RunClusterQueries extends BaseQuery {

    /**
     * Builds the list of queries for Elasticsearch version that was retrieved previously,
     * then executes them and saves the result to temporary storage.
     */

    private static final Logger logger = LogManager.getLogger(RunClusterQueries.class);

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
        } catch (Throwable t) {
            logger.error( "Error executing REST queries", t);
            throw new DiagnosticException(String.format("Unrecoverable REST Query Execution error - exiting. %s", Constants.CHECK_LOG));
        }

    }

}
