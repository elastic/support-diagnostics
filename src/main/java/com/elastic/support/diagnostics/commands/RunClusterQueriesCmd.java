package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.Constants;
import com.elastic.support.diagnostics.DiagConfig;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestEntry;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RunClusterQueriesCmd extends BaseQueryCmd {

    /**
     * Builds the list of queries for Elasticsearch version that was retrieved previously,
     * then executes them and saves the result to temporary storage.
     */

    private static final Logger logger = LogManager.getLogger(RunClusterQueriesCmd.class);

    public void execute(DiagnosticContext context) {

        try {
            DiagConfig diagConfig = context.getDiagsConfig();
            List<RestEntry> entries = new ArrayList();
            entries.addAll(context.getElasticRestCalls().values());
            runQueries(context.getEsRestClient(), entries, context.getTempDir(), diagConfig.getCallRetries(), diagConfig.getPauseRetries());
        } catch (Throwable t) {
            logger.log(SystemProperties.DIAG, "Error executing REST queries", t);
            throw new DiagnosticException(String.format("Unrecoverable REST Query Execution error - exiting. %s", Constants.CHECK_LOG));
        }

    }

}
