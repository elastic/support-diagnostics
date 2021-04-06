package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.diagnostics.DiagnosticInputs;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestEntryConfig;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.ResourceCache;
import com.elastic.support.util.SystemProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.vdurmont.semver4j.Semver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Map;


/**
 * {@code CheckKibanaVersion} uses the REST configuration to fetch the version of
 * Kibana from the server.
 *
 * If this request fails, then the rest of the diagnostic cannot process because REST
 *  calls are setup against specific versions and, without having a version, they cannot
 * be setup.
 */
public class CheckKibanaVersion implements Command {

   
    private static final Logger logger = LogManager.getLogger(CheckKibanaVersion.class);

    public void execute(DiagnosticContext context) {

        // Get the version number from the JSON returned
        // by just submitting the host/port combo
        logger.info(Constants.CONSOLE, "Getting Kibana Version.");
        DiagnosticInputs inputs = context.diagnosticInputs;

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
                    context.diagsConfig.connectionTimeout,
                    context.diagsConfig.connectionRequestTimeout,
                    context.diagsConfig.socketTimeout);

           // Add it to the global cache - automatically closed on exit.
            ResourceCache.addRestClient(Constants.restInputHost, restClient);
            context.version = getKibanaVersion(restClient);
            String version = context.version.getValue();
            RestEntryConfig builder = new RestEntryConfig(version);
            Map restCalls = JsonYamlUtils.readYamlFromClasspath(Constants.KIBANA_REST, true);

            // logger.info(Constants.CONSOLE, restCalls);
            logger.info(Constants.CONSOLE, "Run basic queries for Kibana: {}", restCalls);

            context.elasticRestCalls = builder.buildEntryMap(restCalls);

        } catch (DiagnosticException de) {
            throw de;
        } catch (Exception e) {
            logger.error( "Unanticipated error:", e);
            String errorLog = "Could't retrieve Kibana version due to a system or network error. %s%s%s";
            errorLog = String.format(errorLog, e.getMessage(),
                                    SystemProperties.lineSeparator,
                                    Constants.CHECK_LOG);
            throw new DiagnosticException(errorLog);
        }
    }

   /**
    * Fetch the Kibana version using the {@code client}, which is used to then to determine which
    * REST endpoints can be used from the diagnostic.
    *
    * @param client The configured client to connect to Kibana.
    * @return The Kibana version (semver).
    * @throws DiagnosticException if the request fails or the version is invalid
    */
    public static Semver getKibanaVersion(RestClient client){
            RestResult res = client.execQuery("/api/settings");
            if (! res.isValid()) {
                throw new DiagnosticException(res.formatStatusMessage("Could not retrieve the Kibana version - unable to continue."));
            }
            String result = res.toString();
            JsonNode root = JsonYamlUtils.createJsonNodeFromString(result);
            String version = root.path("settings").path("kibana").path("version").asText();
            logger.info(Constants.CONSOLE, String.format("Kibana Version is :%s", version));

            Pattern versionPattern = Pattern.compile("^([1-9]\\d*)\\.(\\d+)\\.(\\d+)?");
            Matcher matcher = versionPattern.matcher(version);

            if(!matcher.matches()) {
                throw new DiagnosticException(String.format("Kibana version format is wrong - unable to continue. (%s)", version));
            }
            return new Semver(version, Semver.SemverType.NPM);
    }

}
