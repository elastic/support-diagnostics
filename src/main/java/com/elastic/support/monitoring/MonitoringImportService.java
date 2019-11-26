package com.elastic.support.monitoring;

import com.elastic.support.rest.ElasticRestClientService;
import com.elastic.support.Constants;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestEntry;
import com.elastic.support.util.ArchiveUtils;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class MonitoringImportService extends ElasticRestClientService {

    private Logger logger = LogManager.getLogger(MonitoringImportService.class);
    private static final String SCROLL_ID = "{ \"scroll_id\" : \"{{scrollId}}\" }";

    void execImport(MonitoringImportInputs inputs){

        RestClient client = null;
        try {
            Map configMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
            MonitoringExportConfig monitoringExtractConfig = new MonitoringExportConfig(configMap);
            client = createEsRestClient(monitoringExtractConfig, inputs);

            String tempDir = SystemProperties.userDir + SystemProperties.fileSeparator + Constants.MONITORING_DIR;

            // Create the temp directory - delete if first if it exists from a previous run
            logger.info("Creating temp directory: {}", tempDir);

            FileUtils.deleteDirectory(new File(tempDir));
            Files.createDirectories(Paths.get(tempDir));

            // Set up the log file manually since we're going to package it with the diagnostic.
            // It will go to wherever we have the temp dir set up.
            logger.info("Configuring log file.");
            createFileAppender(tempDir, "import.log");
            ArchiveUtils archiveUtils = new ArchiveUtils(new MonitoringImportProcessor(monitoringExtractConfig, inputs, client));
            archiveUtils.extractDiagnosticArchive(inputs.input);


        }catch (Throwable t){
            logger.log(SystemProperties.DIAG, "Error extracting archive or indexing results", t);
            logger.error("Cannot contiue processing. Exiting {}", Constants.CHECK_LOG);
        }
        finally {
            closeLogs();
            client.close();
        }
    }


}
