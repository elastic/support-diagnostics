/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.monitoring;

import co.elastic.support.diagnostics.commands.CheckElasticsearchVersion;
import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.rest.ElasticRestClientService;
import co.elastic.support.Constants;
import co.elastic.support.rest.RestClient;
import co.elastic.support.util.ArchiveUtils;
import co.elastic.support.util.JsonYamlUtils;
import co.elastic.support.util.SystemProperties;
import co.elastic.support.util.SystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semver4j.Semver;

import java.io.File;
import java.util.Map;
import java.util.Vector;

public class MonitoringImportService extends ElasticRestClientService {
    private Logger logger = LogManager.getLogger(MonitoringImportService.class);

    void execImport(MonitoringImportInputs inputs) throws DiagnosticException {
        Map<String, Object> configMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
        MonitoringImportConfig config = new MonitoringImportConfig(configMap);

        try (RestClient client = getClient(inputs, config)){

            String tempDir = SystemProperties.userDir + SystemProperties.fileSeparator + Constants.MONITORING_DIR;
            String extractDir = tempDir + SystemProperties.fileSeparator +"extract";

            // Create the temp directory - delete if first if it exists from a previous run
            SystemUtils.nukeDirectory(tempDir);
            logger.info(Constants.CONSOLE, "Creating temporary directory {}", tempDir);
            new File(extractDir).mkdirs();

            // Set up the log file manually since we're going to package it with the diagnostic.
            // It will go to wherever we have the temp dir set up.
            logger.info(Constants.CONSOLE, "Configuring log file.");
            createFileAppender(tempDir, "import.log");

            // Check the version.
            Semver version = CheckElasticsearchVersion.getElasticsearchVersion(client);
            if (version.getMajor() < 7) {
                throw new DiagnosticException("Target cluster must be at least 7.x");
            }

            ArchiveUtils.extractArchive(inputs.input, extractDir);
            MonitoringImportProcessor processor = new MonitoringImportProcessor(config, inputs, client);
            processor.exec(getDirectoryEntries(extractDir));

        }catch (Exception e){
            logger.error( "Error extracting archive or indexing results", e);
            logger.info(Constants.CONSOLE, "Cannot continue processing. {} \n {}", e.getMessage(), Constants.CHECK_LOG);
        }
        finally {
            closeLogs();
        }
    }

    private Vector<File> getDirectoryEntries(String dir) {
        File targetDir = new File(dir);
        Vector<File> files = new Vector<>();
        files.addAll(FileUtils.listFiles(targetDir, null, true));
        return files;
    }

    private RestClient getClient(MonitoringImportInputs inputs, MonitoringImportConfig config){

        return RestClient.getClient(
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
                config.socketTimeout
        );

    }


}
