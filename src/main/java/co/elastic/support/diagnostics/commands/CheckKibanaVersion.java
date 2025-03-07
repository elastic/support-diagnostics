/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.Constants;
import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.diagnostics.chain.Command;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.rest.RestClient;
import co.elastic.support.rest.RestEntryConfig;
import co.elastic.support.rest.RestResult;
import co.elastic.support.util.JsonYamlUtils;
import co.elastic.support.util.SystemProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semver4j.Semver;
import org.semver4j.SemverException;

import java.util.Map;

/**
 * {@code CheckKibanaVersion} uses the REST configuration to fetch the version
 * of
 * Kibana from the server.
 *
 * If this request fails, then the rest of the diagnostic cannot process because
 * REST
 * calls are setup against specific versions and, without having a version, they
 * cannot
 * be setup.
 */
public class CheckKibanaVersion implements Command {

    private static final Logger logger = LogManager.getLogger(CheckKibanaVersion.class);

    public void execute(DiagnosticContext context) throws DiagnosticException {

        // Get the version number from the JSON returned
        // by just submitting the host/port combo
        logger.info(Constants.CONSOLE, "Getting Kibana Version.");

        try {
            RestClient restClient = RestClient.getClient(
                    context.diagnosticInputs.host,
                    context.diagnosticInputs.port,
                    context.diagnosticInputs.scheme,
                    context.diagnosticInputs.user,
                    context.diagnosticInputs.password,
                    context.diagnosticInputs.proxyHost,
                    context.diagnosticInputs.proxyPort,
                    context.diagnosticInputs.proxyUser,
                    context.diagnosticInputs.proxyPassword,
                    context.diagnosticInputs.pkiKeystore,
                    context.diagnosticInputs.pkiKeystorePass,
                    context.diagnosticInputs.skipVerification,
                    context.diagsConfig.extraHeaders,
                    context.diagsConfig.connectionTimeout,
                    context.diagsConfig.connectionRequestTimeout,
                    context.diagsConfig.socketTimeout);

            // Add it to the global cache - automatically closed on exit.
            context.resourceCache.addRestClient(Constants.restInputHost, restClient);
            context.version = getKibanaVersion(restClient);
            String version = context.version.getVersion();
            RestEntryConfig builder = new RestEntryConfig(version);
            Map restCalls = JsonYamlUtils.readYamlFromClasspath(Constants.KIBANA_REST, true);

            // logger.info(Constants.CONSOLE, restCalls);
            logger.info(Constants.CONSOLE, "Run basic queries for Kibana: {}", restCalls);

            context.elasticRestCalls = builder.buildEntryMap(restCalls);
            context.fullElasticRestCalls = context.elasticRestCalls;
        } catch (Exception e) {
            logger.error("Unanticipated error:", e);
            String errorLog = "Could't retrieve Kibana version due to a system or network error. %s%s%s";
            errorLog = String.format(errorLog, e.getMessage(),
                    SystemProperties.lineSeparator,
                    Constants.CHECK_LOG);
            throw new DiagnosticException(errorLog);
        }
    }

    /**
     * Fetch the Kibana version using the {@code client}, which is used to then to
     * determine which
     * REST endpoints can be used from the diagnostic.
     *
     * @param client The configured client to connect to Kibana.
     * @return The Kibana version (semver).
     * @throws DiagnosticException if the request fails or the version is invalid
     */
    public static Semver getKibanaVersion(RestClient client) throws DiagnosticException {
        RestResult res = client.execQuery("/api/stats");
        if (!res.isValid()) {
            throw new DiagnosticException(
                    res.formatStatusMessage("Could not retrieve the Kibana version - unable to continue."));
        }
        String result = res.toString();
        JsonNode root = JsonYamlUtils.createJsonNodeFromString(result);
        String version = root.path("kibana").path("version").asText();

        logger.info(Constants.CONSOLE, String.format("Kibana Version is :%s", version));

        try {
            // Workaround for the semver issue with pre-release versions
            // https://github.com/semver4j/semver4j/issues/307
            return new Semver(version).withClearedPreRelease();
        } catch (SemverException ex) {
            throw new DiagnosticException(
                    String.format("Kibana version format is wrong - unable to continue. (%s)", version));
        }
    }

}
