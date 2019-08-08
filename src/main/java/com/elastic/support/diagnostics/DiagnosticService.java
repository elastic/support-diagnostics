package com.elastic.support.diagnostics;

import com.elastic.support.BaseService;
import com.elastic.support.config.Constants;
import com.elastic.support.config.DiagConfig;
import com.elastic.support.config.DiagnosticInputs;
import com.elastic.support.diagnostics.chain.DiagnosticChainExec;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestClientBuilder;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

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
        DiagnosticContext ctx = new DiagnosticContext();

        try {

            // Create the temp directory - delete if first if it exists from a previous run
            String tempDir = diagnosticInputs.getTempDir();
            logger.info("Creating temp directory: {}", tempDir);

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
            ctx.setEsRestClient(esRestClient);
            ctx.setGenericClient(genericClient);
            ctx.setDiagsConfig(diagConfig);
            ctx.setDiagnosticInputs(diagnosticInputs);
            ctx.setTempDir(tempDir);

            dc.runDiagnostic(ctx, chain);

            if (ctx.isDocker()){
                logger.warn("Identified Docker installations - bypassed log collection and system calls.");
            }

            String user = ctx.getDiagnosticInputs().getUser();

            if (StringUtils.isNotEmpty(user) && !ctx.isAuthorized()) {

                String border = SystemUtils.buildStringFromChar(60, '*');
                logger.warn(SystemProperties.lineSeparator);
                logger.warn(border);
                logger.warn(border);
                logger.warn(border);
                logger.warn("The elasticsearch user entered: {} does not appear to have sufficient authorization to access all collected information", user);
                logger.warn("Some of the calls may not have completed successfully.");
                logger.warn("If you are using a custom role please verify that it has the admin role for versions prior to 5.x or the superuser role for subsequent versions.");
                logger.warn(border);
                logger.warn(border);
                logger.warn(border);

            }
        } catch (DiagnosticException de) {
            logger.warn(de.getMessage());
        } catch (Throwable t) {
            logger.log(SystemProperties.DIAG, "Temp directory error" , t);
            logger.warn(String.format("Issue with creating temp directory. %s", Constants.CHECK_LOG));
        } finally {
            closeLogs();
            createArchive(ctx.getTempDir());
            SystemUtils.nukeDirectory(ctx.getTempDir());
            esRestClient.close();
            genericClient.close();
        }
    }

    private RestClient createGenericClient(DiagConfig diagConfig, DiagnosticInputs diagnosticInputs) {
        RestClientBuilder builder = new RestClientBuilder();

        return builder
                .setConnectTimeout(diagConfig.getRestConfig().get("connectTimeout") * 1000)
                .setRequestTimeout(diagConfig.getRestConfig().get("requestTimeout") * 1000)
                .setSocketTimeout(diagConfig.getRestConfig().get("socketTimeout") * 1000)
                .setProxyHost(diagnosticInputs.getProxyUser())
                .setProxPort(diagnosticInputs.getProxyPort())
                .setProxyUser(diagnosticInputs.getUser())
                .setProxyPass(diagnosticInputs.getProxyPassword())
                .build();

    }

    private RestClient createEsRestClient(DiagConfig diagConfig, DiagnosticInputs diagnosticInputs) {
        RestClientBuilder builder = new RestClientBuilder();
        builder
                .setConnectTimeout(diagConfig.getRestConfig().get("connectTimeout") * 1000)
                .setRequestTimeout(diagConfig.getRestConfig().get("requestTimeout") * 1000)
                .setSocketTimeout(diagConfig.getRestConfig().get("socketTimeout") * 1000)
                .setProxyHost(diagnosticInputs.getProxyUser())
                .setProxPort(diagnosticInputs.getProxyPort())
                .setProxyUser(diagnosticInputs.getUser())
                .setProxyPass(diagnosticInputs.getProxyPassword())
                .setBypassVerify(diagnosticInputs.isSkipVerification())
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
