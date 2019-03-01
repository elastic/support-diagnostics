package com.elastic.support.diagnostics;

import com.elastic.support.BaseService;
import com.elastic.support.config.DiagConfig;
import com.elastic.support.config.DiagnosticInputs;
import com.elastic.support.diagnostics.chain.DiagnosticChainExec;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestClientBuilder;
import com.elastic.support.util.ArchiveUtils;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class DiagnosticService extends BaseService {


    public void exec(DiagnosticInputs diagnosticInputs, DiagConfig diagConfig, Map<String, List<String>> chains) {

        // Create two clients, one generic for Github and one customized for this ES cluster
        // We use the same connection manager for both to pool all connections.
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(25);
        connectionManager.setDefaultMaxPerRoute(5);

        RestClientBuilder builder = new RestClientBuilder();
        RestClient genericClient = builder
                .setClientTimeouts(diagConfig.getRestConfig().get("requestTimeout"),
                        diagConfig.getRestConfig().get("connectTimeout"),
                        diagConfig.getRestConfig().get("socketTimeout"))
                .setConnectionManager(connectionManager)
                .build();

        builder = builder.setBypassVerify(diagnosticInputs.isBypassDiagVerify());

        if (diagnosticInputs.isProxy()) {
            builder = builder.setProxy(diagnosticInputs.getProxyHost(), diagnosticInputs.getProxyPort());
            if (diagnosticInputs.isProtectedProxy()) {
                builder = builder.setProxyAuth(
                        diagnosticInputs.getProxyHost(), diagnosticInputs.getProxyPort(),
                        diagnosticInputs.getProxyUser(), diagnosticInputs.getProxyPassword());
            }
        }

        if (diagnosticInputs.isPki()) {
            builder.setPkiKeystore(diagnosticInputs.getPkiKeystore(), diagnosticInputs.getPkiKeystorePass());
        }

        RestClient esRestClient = builder.build();

        try {
            // Create the temp directory - delete if first if it exists from a previous run
            String tempDir = diagnosticInputs.getTempDir();
            logger.info("Creating temp directory: {}", tempDir);
            Map<String, Integer> restConfig = diagConfig.getRestConfig();

            FileUtils.deleteDirectory(new File(tempDir));
            Files.createDirectories(Paths.get(tempDir));

            logger.info("Created temp directory: {}", diagnosticInputs.getTempDir());

            // Set up the log file manually since we're going to package it with the diagnostic.
            // It will go to wherever we have the temp dir set up.
            logger.info("Configuring log file.");
            createFileAppender(tempDir, "diagnostics.log");

            // Get the type of diag we're running and use it to find the right chain
            List<String> chain = chains.get(diagnosticInputs.getDiagType());

            DiagnosticChainExec dc = new DiagnosticChainExec();
            DiagnosticContext ctx = new DiagnosticContext();


            ctx.setEsRestClient(esRestClient);
            ctx.setGenericClient(genericClient);
            ctx.setDiagsConfig(diagConfig);

            dc.runDiagnostic(ctx, chain);

            closeLogs();
            createArchive(ctx);
            SystemUtils.nukeDirectory(tempDir);

        } catch (IOException e) {
            logger.error("Access issue with temp directory", e);
            throw new RuntimeException("Issue with creating temp directory - see logs for details.");
        } finally {
            closeLogs();
            esRestClient.close();
            genericClient.close();
            connectionManager.close();
        }
    }

    private void createArchive(DiagnosticContext context) {

        logger.info("Archiving diagnostic results.");

        try {
            String archiveFilename = SystemProperties.getFileDateString();
            String dir = context.getDiagnosticInputs().getTempDir();
            ArchiveUtils archiveUtils = new ArchiveUtils();
            archiveUtils.createArchive(dir, archiveFilename);

        } catch (Exception ioe) {
            logger.error("Couldn't create archive. {}", ioe);
        }
    }

}
