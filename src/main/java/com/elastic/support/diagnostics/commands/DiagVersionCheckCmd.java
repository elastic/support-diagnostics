package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.DiagConfig;
import com.elastic.support.config.DiagnosticInputs;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestExec;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class DiagVersionCheckCmd implements Command {

    private final Logger logger = LogManager.getLogger(DiagVersionCheckCmd.class);

    public void execute(DiagnosticContext context) {

        DiagnosticInputs diagnosticInputs = context.getDiagnosticInputs();

        if (diagnosticInputs.isBypassDiagVerify()) {
            return;
        }

        logger.info("Checking for diagnostic version updates.");

        try {
            DiagConfig diagConfig = context.getDiagsConfig();
            Map<String, String> ghSettings = diagConfig.getGithubSettings();

            String ghHost = (String) ghSettings.get("diagReleaseHost");
            String ghEndpoint = (String) ghSettings.get("diagReleaseDest");
            String ghScheme = (String) ghSettings.get("diagReleaseScheme");

            RestClient restClient = context.getGenericClient();
            restClient.configureDestination(ghHost, RestClient.DEEFAULT_HTTPS_PORT, ghScheme);

            String diagVersion = context.getDiagVersion();
            if (diagVersion.equalsIgnoreCase("debug")) {
                logger.info("Running in debugger - bypassing check");
                return;
            }

            String result = restClient.execQuery(ghEndpoint).toString();
            JsonNode rootNode = JsonYamlUtils.createJsonNodeFromString(result);
            String ver = rootNode.path("tag_name").asText();
            List<JsonNode> assests = rootNode.findValues("assets");
            JsonNode asset = assests.get(0);
            String downloadUrl = asset.path("browser_download_url").asText();

            if (!diagVersion.equals(ver)) {
                logger.info("Warning: DiagnosticService version:{} is not the current recommended release", diagVersion);
                logger.info("The current release is {}", ver);
                logger.info("The latest version can be downloaded at {}", downloadUrl);

                Scanner sc = new Scanner(System.in);
                String feedback = null;
                boolean valid = false;
                System.out.println("Continue anyway? [Y/N]");
                feedback = sc.nextLine();
                while (!valid) {
                    if ("y".equalsIgnoreCase(feedback) || "n".equalsIgnoreCase(feedback)) {
                        valid = true;
                    } else {
                        System.out.println("Continue anyway? [Y/N]");
                        feedback = sc.nextLine();
                    }
                }

                if ("n".equalsIgnoreCase(feedback)) {
                    System.out.println("Exiting application");
                }
            }
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, e);
            logger.info("Issue encountered while checking diagnostic version for updates.");
            logger.info("Failed to get current diagnostic version from Github.");
            logger.info("If Github is not accessible from this environemnt current supported version cannot be confirmed.");
        }

    }
}
