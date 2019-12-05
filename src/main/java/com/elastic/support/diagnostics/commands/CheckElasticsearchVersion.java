package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestEntry;
import com.elastic.support.rest.RestEntryFactory;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.vdurmont.semver4j.Semver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;


public class CheckElasticsearchVersion implements Command {

    /**
     * Gets the version of Elasticsearch that is running. This also
     * acts as a sanity check. If there are connection issues and it fails
     * this will bet the first indication since this is lightweight enough
     * that is should usually succeed. If we don't have a version we
     * won't be able to generate the correct call selection later on.
     */
    private final Logger logger = LogManager.getLogger(CheckElasticsearchVersion.class);

    public void execute(DiagnosticContext context) {

        // Get the version number from the JSON returned
        // by just submitting the host/port combo
        logger.info("Getting Elasticsearch Version.");

        try {
            RestClient restClient = context.getEsRestClient();
            RestResult res = restClient.execQuery("/");
            if (! res.isValid()) {
                throw new DiagnosticException( res.formatStatusMessage( "Could not retrieve the Elasticsearch version - unable to continue."));
            }
            String result = res.toString();
            JsonNode root = JsonYamlUtils.createJsonNodeFromString(result);
            String version = root.path("version").path("number").asText();
            context.setVersion(getElasticsearchVersion(restClient));

            RestEntryFactory builder = new RestEntryFactory(version);

            Map restCalls = JsonYamlUtils.readYamlFromClasspath(Constants.ES_REST, true);
            Map<String, RestEntry> entries = builder.buildEntryMap(restCalls);
            context.setElasticRestCalls(entries);

            restCalls = JsonYamlUtils.readYamlFromClasspath(Constants.LS_REST, true);
            entries = builder.buildEntryMap(restCalls);
            context.setLogstashRestCalls(entries);

        } catch (DiagnosticException de) {
            throw de;
        } catch (Throwable t) {
            logger.log(SystemProperties.DIAG, "Unanticipated error:", t);
            throw new DiagnosticException(String.format("Could not retrieve the Elasticsearch version due to a system or network error - unable to continue. %s", Constants.CHECK_LOG));
        }
    }

    public static Semver getElasticsearchVersion(RestClient client){
        RestResult res = client.execQuery("/");
        if (! res.isValid()) {
            throw new DiagnosticException( res.formatStatusMessage( "Could not retrieve the Elasticsearch version - unable to continue."));
        }
        String result = res.toString();
        JsonNode root = JsonYamlUtils.createJsonNodeFromString(result);
        String version = root.path("version").path("number").asText();
        return new Semver(version, Semver.SemverType.NPM);

    }
}
