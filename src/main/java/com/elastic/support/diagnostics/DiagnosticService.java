package com.elastic.support.diagnostics;

import com.elastic.support.rest.ElasticRestClientService;
import com.elastic.support.Constants;
import com.elastic.support.diagnostics.chain.DiagnosticChainExec;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DiagnosticService extends ElasticRestClientService {

    private Logger logger = LogManager.getLogger(DiagnosticService.class);

    public void exec(DiagnosticInputs diagnosticInputs, DiagConfig diagConfig) {

        // Create two clients, one generic for Github or any other external site and one customized for this ES cluster
        RestClient genericClient = createGenericClient(diagConfig, diagnosticInputs);
        RestClient esRestClient = createEsRestClient(diagConfig, diagnosticInputs);
        DiagnosticContext ctx = new DiagnosticContext();

        try {

            // Create the temp directory - delete if first if it exists from a previous run
            String outputDir = diagnosticInputs.getOutputDir();
            String tempDir;
            if (diagnosticInputs.getDiagType().equals(Constants.ES_DIAG_DEFAULT)) {
                tempDir = outputDir + SystemProperties.fileSeparator + Constants.ES_DIAG;
            } else {
                tempDir = outputDir + SystemProperties.fileSeparator + diagnosticInputs.getDiagType() + "-" + Constants.ES_DIAG;
            }

            logger.info("Creating temp directory: {}", tempDir);

            FileUtils.deleteDirectory(new File(tempDir));
            Files.createDirectories(Paths.get(tempDir));

            // Set up the log file manually since we're going to package it with the diagnostic.
            // It will go to wherever we have the temp dir set up.
            logger.info("Configuring log file.");
            createFileAppender(tempDir, "diagnostics.log");

            ctx.setEsRestClient(createEsRestClient(diagConfig, diagnosticInputs));
            ctx.setGenericClient(createGenericClient(diagConfig, diagnosticInputs));
            ctx.setDiagsConfig(diagConfig);
            ctx.setDiagnosticInputs(diagnosticInputs);
            ctx.setTempDir(tempDir);

            DiagnosticChainExec.runDiagnostic(ctx, diagnosticInputs.getDiagType());

            if (ctx.isDocker()) {
                logger.warn("Identified Docker installations - bypassed log collection and system calls.");
            }

           checkAuthLevel(ctx.getDiagnosticInputs().getUser(), ctx.isAuthorized());

        } catch (DiagnosticException de) {
            logger.warn(de.getMessage());
        } catch (Throwable t) {
            logger.log(SystemProperties.DIAG, "Temp directory error", t);
            logger.warn(String.format("Issue with creating temp directory. %s", Constants.CHECK_LOG));
        } finally {
            closeLogs();
            createArchive(ctx.getTempDir());
            SystemUtils.nukeDirectory(ctx.getTempDir());
            esRestClient.close();
            genericClient.close();
        }
    }





}
