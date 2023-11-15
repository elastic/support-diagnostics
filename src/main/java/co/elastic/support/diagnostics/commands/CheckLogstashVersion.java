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
import co.elastic.support.diagnostics.DiagnosticInputs;
import co.elastic.support.diagnostics.chain.Command;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.rest.RestClient;
import co.elastic.support.rest.RestEntryConfig;
import co.elastic.support.rest.RestResult;
import co.elastic.support.util.JsonYamlUtils;
import co.elastic.support.util.SystemProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.vdurmont.semver4j.Semver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class CheckLogstashVersion implements Command {

    /**
     * Gets the version of Logstash that is running. This also
     * acts as a sanity check. If there are connection issues and it fails
     * this will bet the first indication since this is lightweight enough
     * that is should usually succeed. If we don't have a version we
     * won't be able to generate the correct call selection later on.
     */
    private static final Logger logger = LogManager.getLogger(CheckLogstashVersion.class);

    public void execute(DiagnosticContext context) throws DiagnosticException {

        // Get the version number from the JSON returned
        // by just submitting the host/port combo
        logger.info(Constants.CONSOLE, "Getting Logstash Version.");
        DiagnosticInputs inputs = context.diagnosticInputs;
        DiagConfig config = context.diagsConfig;

        try {
            RestClient restClient = RestClient.getClient(
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

            // Add it to the global cache - automatically closed on exit.
            context.resourceCache.addRestClient(Constants.restInputHost, restClient);
            context.version = getLogstashVersion(restClient);
            String version = context.version.getValue();
            RestEntryConfig builder = new RestEntryConfig(version, inputs.mode);
            Map restCalls = JsonYamlUtils.readYamlFromClasspath(Constants.ES_REST, true);

            context.elasticRestCalls = builder.buildEntryMap(restCalls);
            // For Logstash, we use some API calls based
            context.fullElasticRestCalls = new RestEntryConfig(version).buildEntryMap(restCalls);
        } catch (Exception e) {
            logger.error("Unanticipated error:", e);
            throw new DiagnosticException(String.format(
                    "Could not retrieve the Logstash version due to a system or network error - unable to continue. %s%s%s",
                    e.getMessage(), SystemProperties.lineSeparator, Constants.CHECK_LOG));
        }
    }

    public static Semver getLogstashVersion(RestClient client) throws DiagnosticException {
        RestResult res = client.execQuery("/");
        if (!res.isValid()) {
            throw new DiagnosticException(
                    res.formatStatusMessage("Could not retrieve the Logstash version - unable to continue."));
        }
        String result = res.toString();
        JsonNode root = JsonYamlUtils.createJsonNodeFromString(result);
        String version = root.path("version").asText();
        return new Semver(version, Semver.SemverType.NPM);
    }
}
