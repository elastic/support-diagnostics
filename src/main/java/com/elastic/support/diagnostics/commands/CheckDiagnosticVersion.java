package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagConfig;
import com.elastic.support.diagnostics.DiagnosticInputs;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Scanner;


public class CheckDiagnosticVersion implements Command {

    private final Logger logger = LogManager.getLogger(CheckDiagnosticVersion.class);

    /**
     * Checks the embedded version of the current diagnostic created when the jar was built
     * with the release version of the current deployment on Github.
     * If it is not that version, prompt the user on whether to continue.
     * It will also provide a download URL for the current version.
     */

    public void execute(DiagnosticContext context) {

        // For airgapped environments allow them to bypass this check
        if (context.diagnosticInputs.bypassDiagVerify) {
            return;
        }

        logger.info("Checking for diagnostic version updates.");
        // Only need this once so let it auto-close at the end of the try catch block.
        try(RestClient restClient = RestClient.getClient(
                    context.diagsConfig.diagReleaseHost,
                    Constants.DEEFAULT_HTTPS_PORT,
                    context.diagsConfig.diagReleaseScheme,
                    "",
                    "",
                    "",
                    0,
                    "",
                    "",
                    "",
                    "",
                    true,
                    context.diagsConfig.connectionTimeout,
                    context.diagsConfig.connectionRequestTimeout,
                    context.diagsConfig.socketTimeout)){

            // Get the current diagnostic version that was populated in the
            // manifest generation step - if we're running in
            // the IDE via a run configuration and/or debugger it will
            // have a value of "debug" instead of an actual version.
            context.diagVersion = getToolVersion();
            if (StringUtils.isEmpty(context.diagVersion)) {
                throw new RuntimeException("Empty diagnostic version.");
            }

            if (context.diagVersion.equalsIgnoreCase("debug")) {
                logger.info("Running in debugger - bypassing check");
                return;
            }

            RestResult restResult = new RestResult(restClient.execGet(
                    context.diagsConfig.diagReleaseDest), context.diagsConfig.diagReleaseDest);
            JsonNode rootNode = JsonYamlUtils.createJsonNodeFromString(restResult.toString());
            String ver = rootNode.path("tag_name").asText();
            List<JsonNode> assests = rootNode.findValues("assets");
            JsonNode asset = assests.get(0);
            String downloadUrl = asset.path("browser_download_url").asText();

            if (!context.diagVersion.equals(ver)) {
                logger.info("Warning: DiagnosticService version:{} is not the current recommended release", context.diagVersion);
                logger.info("The current release is {}", ver);
                logger.info("The latest version can be downloaded at {}", downloadUrl);
                logger.info("Press the Enter key to continue.");

                Scanner sc = new Scanner(System.in);
                sc.nextLine();
            }

        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, e);
            logger.info("Issue encountered while checking diagnostic version for updates.");
            logger.info("Failed to get current diagnostic version from Github.");
            logger.info("If Github is not accessible from this environemnt current supported version cannot be confirmed.");
        }
    }

    public String getToolVersion() {
        String ver = GenerateManifest.class.getPackage().getImplementationVersion();
        return (ver != null) ? ver : "Debug";
    }


}
