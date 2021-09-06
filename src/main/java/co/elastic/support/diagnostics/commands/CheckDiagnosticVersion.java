/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.rest.RestResult;
import co.elastic.support.util.JsonYamlUtils;
import co.elastic.support.Constants;
import co.elastic.support.diagnostics.chain.Command;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.rest.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.vdurmont.semver4j.Semver;
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

        logger.info(Constants.CONSOLE, "Checking for diagnostic version updates.");
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
                    context.diagsConfig.extraHeaders,
                    context.diagsConfig.connectionTimeout,
                    context.diagsConfig.connectionRequestTimeout,
                    context.diagsConfig.socketTimeout)){

            // Get the current diagnostic version that was populated in the
            // manifest generation step - if we're running in
            // the IDE via a run configuration and/or debugger it will
            // have a value of "debug" instead of an actual version.
            context.diagVersion = getToolVersion();
            if (context.diagVersion.equals(Constants.runningInIde) || StringUtils.isEmpty(context.diagVersion)) {
                logger.info(Constants.CONSOLE, "Running in IDE");
                // Default it to something that won't blow up the Semver but shows it's not a normal run.
                context.diagVersion = "0.0.0";
                return;
            }

            RestResult restResult = new RestResult(restClient.execGet(
                    context.diagsConfig.diagLatestRelease), context.diagsConfig.diagLatestRelease);
            JsonNode rootNode = JsonYamlUtils.createJsonNodeFromString(restResult.toString());
            String ver = rootNode.path("tag_name").asText();
            Semver diagVer = new Semver(context.diagVersion, Semver.SemverType.NPM);
            String rule = ">= " + ver;

            if (!diagVer.satisfies(rule)) {

                logger.info(Constants.CONSOLE, "Warning: DiagnosticService version:{} is not the current recommended release", context.diagVersion);
                logger.info(Constants.CONSOLE, "The current release is {}", ver);

                // Try to get the link for the download url of the current release.
                List<JsonNode> assets = rootNode.findValues("assets");
                JsonNode asset = assets.get(0);
                ArrayNode attachments = null;
                if(asset.isArray()){
                    attachments = (ArrayNode)asset;
                    asset = attachments.get(0);
                }
                String downloadUrl = asset.path("browser_download_url").asText();
                if(StringUtils.isEmpty(downloadUrl)){
                    downloadUrl = context.diagsConfig.diagLatestRelease;
                }

                logger.info(Constants.CONSOLE, "The latest version can be downloaded at {}", downloadUrl);
                logger.info(Constants.CONSOLE, "Press the Enter key to continue.");

                Scanner sc = new Scanner(System.in);
                sc.nextLine();
            }

        } catch (Exception e) {
            logger.error( e);
            logger.info(Constants.CONSOLE, "Issue encountered while checking diagnostic version for updates.");
            logger.info(Constants.CONSOLE, "Failed to get current diagnostic version from Github.");
            logger.info(Constants.CONSOLE, "If Github is not accessible from this environment current supported version cannot be confirmed.");
        }
    }

    public String getToolVersion() {
        String ver = GenerateManifest.class.getPackage().getImplementationVersion();
        return (ver != null) ? ver : Constants.runningInIde;
    }


}
