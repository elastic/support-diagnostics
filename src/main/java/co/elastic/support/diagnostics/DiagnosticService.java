/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics;

import co.elastic.support.rest.ElasticRestClientService;
import co.elastic.support.util.SystemProperties;
import co.elastic.support.util.SystemUtils;
import co.elastic.support.Constants;
import co.elastic.support.diagnostics.chain.DiagnosticChainExec;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.rest.RestClient;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DiagnosticService extends ElasticRestClientService {

    private Logger logger = LogManager.getLogger(DiagnosticService.class);

    public File exec(DiagnosticContext context) throws DiagnosticException {
        DiagConfig config = context.diagsConfig;
        DiagnosticInputs inputs = context.diagnosticInputs;
        File file;

        try (
                RestClient esRestClient = RestClient.getClient(
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
                        config.socketTimeout)) {

            context.resourceCache.addRestClient(Constants.restInputHost, esRestClient);

            // Create the temp directory - delete if first if it exists from a previous run
            String outputDir = inputs.outputDir;
            context.tempDir = outputDir + SystemProperties.fileSeparator + inputs.diagType + "-" + Constants.ES_DIAG;
            logger.info(Constants.CONSOLE, "{}Creating temp directory: {}", SystemProperties.lineSeparator,
                    context.tempDir);

            try {
                FileUtils.deleteDirectory(new File(context.tempDir));
                Files.createDirectories(Paths.get(context.tempDir));
            } catch (IOException ioe) {
                logger.error("Temp directory error", ioe);
                logger.info(Constants.CONSOLE,
                        String.format("Issue with creating temp directory. %s", Constants.CHECK_LOG));
                throw new DiagnosticException("Could not create temporary directory", ioe);
            }

            // Modify the log file setup since we're going to package it with the
            // diagnostic.
            // The log4 configuration file sets up 2 loggers, one strictly for the console
            // and a file log in the working directory to handle
            // any errors we get at the library level that occur before we can get it
            // initiailized. When we have a target directory to
            // redirect output to we can reconfigure the appender to go to the diagnostic
            // output temp directory for packaging with the archive.
            // This lets you configure and create loggers via the file if you want to up the
            // level on one of the library dependencies as well
            // as internal classes.
            // If you want the output to also be shown on the console use:
            // logger.info/error/warn/debug(Constants.CONSOLE, "Some log message");
            // This will also log that same output to the diagnostic log file.
            // To just log to the file log as normal: logger.info/error/warn/debug("Log
            // mewssage");

            if (context.includeLogs) {
                logger.info(Constants.CONSOLE, "Configuring log file.");
                createFileAppender(context.tempDir, "diagnostics.log");
            }
            DiagnosticChainExec.runDiagnostic(context, inputs.diagType);

            if (context.dockerPresent) {
                logger.info(Constants.CONSOLE,
                        "Identified Docker installations - bypassed log collection and some system calls.");
            }

            checkAuthLevel(context.diagnosticInputs.user, context.isAuthorized);
        } finally {
            if (context.includeLogs) {
                closeLogs();
            }
            file = createArchive(context.tempDir);
            SystemUtils.nukeDirectory(context.tempDir);
        }

        return file;
    }
}
