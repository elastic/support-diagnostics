package com.elastic.support.diagnostics;

import com.elastic.support.BaseService;
import com.elastic.support.config.DiagConfig;
import com.elastic.support.config.DiagnosticInputs;
import com.elastic.support.diagnostics.chain.DiagnosticChainExec;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestClientBuilder;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class DiagnosticService extends BaseService {


    public void exec(DiagnosticInputs diagnosticInputs, DiagConfig diagConfig, Map<String, List<String>> chains) {

        // Create two clients, one generic for Github or any other external site and one customized for this ES cluster
        RestClient genericClient = createGenericClient(diagConfig, diagnosticInputs);
        RestClient esRestClient = createEsRestClient(diagConfig, diagnosticInputs);

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
            ctx.setDiagnosticInputs(diagnosticInputs);
            ctx.setTempDir(tempDir);

            dc.runDiagnostic(ctx, chain);

            closeLogs();
            createArchive(ctx.getTempDir());
            SystemUtils.nukeDirectory(tempDir);

        } catch (IOException e) {
            logger.error("Access issue with temp directory", e);
            throw new RuntimeException("Issue with creating temp directory - see logs for details.");
        } finally {
            closeLogs();
            esRestClient.close();
            genericClient.close();
        }
    }

    private RestClient createGenericClient(DiagConfig diagConfig, DiagnosticInputs diagnosticInputs) {
        RestClientBuilder builder = new RestClientBuilder();
        return builder
                .setConnectTimeout(diagConfig.getRestConfig().get("connectTimeout"))
                .setRequestTimeout(diagConfig.getRestConfig().get("requestTimeout"))
                .setSocketTimeout(diagConfig.getRestConfig().get("socketTimeout"))
                .setProxyHost(diagnosticInputs.getProxyUser())
                .setProxPort(diagnosticInputs.getProxyPort())
                .setProxyUser(diagnosticInputs.getUser())
                .setProxyPass(diagnosticInputs.getProxyPassword())
                .build();

    }

    private RestClient createEsRestClient(DiagConfig diagConfig, DiagnosticInputs diagnosticInputs) {
        RestClientBuilder builder = new RestClientBuilder();
        builder
                .setConnectTimeout(diagConfig.getRestConfig().get("connectTimeout"))
                .setRequestTimeout(diagConfig.getRestConfig().get("requestTimeout"))
                .setSocketTimeout(diagConfig.getRestConfig().get("socketTimeout"))
                .setProxyHost(diagnosticInputs.getProxyUser())
                .setProxPort(diagnosticInputs.getProxyPort())
                .setProxyUser(diagnosticInputs.getUser())
                .setProxyPass(diagnosticInputs.getProxyPassword())
                .setBypassVerify(diagnosticInputs.isBypassDiagVerify())
                .setHost(diagnosticInputs.getHost())
                .setPort(diagnosticInputs.getPort())
                .setScheme(diagnosticInputs.getScheme());

        if (diagnosticInputs.isSecured()) {
            builder.setUser(diagnosticInputs.getUser())
                    .setPassword(diagnosticInputs.getPassword());
        }

        if (diagnosticInputs.isPki()) {
            builder.setPkiKeystore(diagnosticInputs.getPkiKeystore())
                    .setPkiKeystorePass(diagnosticInputs.getPkiKeystorePass());
        }

        return builder.build();
    }


}
