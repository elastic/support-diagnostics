package com.elastic.support.monitoring;

import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.diagnostics.commands.CheckElasticsearchVersion;
import com.elastic.support.rest.ElasticRestClientService;
import com.elastic.support.Constants;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestEntry;
import com.elastic.support.util.*;
import com.vdurmont.semver4j.Semver;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Vector;

public class MonitoringImportService extends ElasticRestClientService {

    private Logger logger = LogManager.getLogger(MonitoringImportService.class);
    private static final String SCROLL_ID = "{ \"scroll_id\" : \"{{scrollId}}\" }";

    void execImport(MonitoringImportInputs inputs){

        Map configMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
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
            logger.info(Constants.CONSOLE, "Cannot contiue processing. {} \n {}", e.getMessage(), Constants.CHECK_LOG);
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
                config.connectionTimeout,
                config.connectionRequestTimeout,
                config.socketTimeout
        );

    }


}
