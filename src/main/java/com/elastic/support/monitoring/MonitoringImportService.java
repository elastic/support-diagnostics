package com.elastic.support.monitoring;

import com.elastic.support.BaseService;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.diagnostics.commands.CheckElasticsearchVersion;
import com.elastic.support.Constants;
import com.elastic.support.rest.RestClient;
import com.elastic.support.util.*;
import com.vdurmont.semver4j.Semver;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Vector;

public class MonitoringImportService  implements BaseService {

    private Logger logger = LogManager.getLogger(MonitoringImportService.class);
    private static final String SCROLL_ID = "{ \"scroll_id\" : \"{{scrollId}}\" }";

    MonitoringImportInputs inputs;
    MonitoringImportConfig config;

    public MonitoringImportService(MonitoringImportInputs inputs, MonitoringImportConfig config){
        this.inputs = inputs;
        this.config = config;
    }

    public void exec(){

        try {
            // Check the version.
            Semver version = CheckElasticsearchVersion.getElasticsearchVersion(ResourceUtils.restClient);
            if (version.getMajor() < 7) {
                throw new DiagnosticException("Target cluster must be at least 7.x");
            }

            ArchiveUtils.extractArchive(inputs.input, inputs.tempDir);
            MonitoringImportProcessor processor = new MonitoringImportProcessor(config, inputs, ResourceUtils.restClient);
            processor.exec(getDirectoryEntries(inputs.tempDir));

        }catch (Exception e){
            logger.error( "Error extracting archive or indexing results", e);
            logger.info(Constants.CONSOLE, "Cannot contiue processing. {} \n {}", e.getMessage(), Constants.CHECK_LOG);
        }
        finally {
        }
    }

    private Vector<File> getDirectoryEntries(String dir) {
        File targetDir = new File(dir);
        Vector<File> files = new Vector<>();
        files.addAll(FileUtils.listFiles(targetDir, null, true));
        return files;
    }
}
