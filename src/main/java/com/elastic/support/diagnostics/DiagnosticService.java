package com.elastic.support.diagnostics;

import com.elastic.support.rest.ElasticRestClientService;
import com.elastic.support.Constants;
import com.elastic.support.diagnostics.chain.DiagnosticChainExec;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DiagnosticService extends ElasticRestClientService {

    private Logger logger = LogManager.getLogger(DiagnosticService.class);

    public void exec(DiagnosticInputs inputs, DiagConfig config) {

        DiagnosticContext ctx = new DiagnosticContext();
        ctx.diagsConfig = config;
        ctx.diagnosticInputs = inputs;

        try(
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
                    config.connectionTimeout,
                    config.connectionRequestTimeout,
                    config.socketTimeout
            )){

            ResourceCache.addRestClient(Constants.restInputHost, esRestClient);

            // Create the temp directory - delete if first if it exists from a previous run
            String outputDir = inputs.outputDir;
            ctx.tempDir = outputDir + SystemProperties.fileSeparator + inputs.diagType + "-" + Constants.ES_DIAG;
            logger.info("{}Creating temp directory: {}", SystemProperties.lineSeparator, ctx.tempDir);

            FileUtils.deleteDirectory(new File(ctx.tempDir));
            Files.createDirectories(Paths.get(ctx.tempDir));

            // Set up the log file manually since we're going to package it with the diagnostic.
            // It will go to wherever we have the temp dir set up.
            logger.info("Configuring log file.");
            createFileAppender(ctx.tempDir, "diagnostics.log");

            DiagnosticChainExec.runDiagnostic(ctx, inputs.diagType);

            if (ctx.dockerPresent) {
                logger.info("Identified Docker installations - bypassed log collection and system calls.");
            }

           checkAuthLevel(ctx.diagnosticInputs.user, ctx.isAuthorized);

        } catch (DiagnosticException de) {
            logger.info(de.getMessage());
        } catch (Throwable t) {
            logger.log(SystemProperties.DIAG, "Temp directory error", t);
            logger.info(String.format("Issue with creating temp directory. %s", Constants.CHECK_LOG));
        } finally {
            closeLogs();
            createArchive(ctx.tempDir);
            SystemUtils.nukeDirectory(ctx.tempDir);
            ResourceCache.closeAll();
        }
    }



}
