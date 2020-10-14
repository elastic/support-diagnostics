package com.elastic.support.monitoring;

import com.elastic.support.BaseApp;
import com.elastic.support.Constants;
import com.elastic.support.diagnostics.ShowHelpException;
import com.elastic.support.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class MonitoringExportApp extends BaseApp {

    private static final Logger logger = LogManager.getLogger(MonitoringExportApp.class);

    public static void main(String[] args) {

        try {
            Map configMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
            MonitoringExportConfig config = new MonitoringExportConfig(configMap);
            MonitoringExportInputs inputs = new MonitoringExportInputs(config.delimiter);
            initInputs(args, inputs);
            elasticsearchConnection(inputs, config);
            githubConnection(config);
            ResourceUtils.startLog(inputs.tempDir + SystemProperties.fileSeparator + "diagnostic.log");
            new MonitoringExportService(inputs, config).exec();
            ResourceUtils.closeFileLogs();
            ArchiveUtils.archiveDirectory(inputs.tempDir, "monitoring-export-" + SystemProperties.getFileDateString() + ".zip");
            SystemUtils.nukeDirectory(inputs.tempDir);
        } catch (ShowHelpException she){
            SystemUtils.quitApp();
        } catch (Exception e) {
            logger.error(Constants.CONSOLE, "Fatal error occurred: {}. {}", e.getMessage(), Constants.CHECK_LOG);
        } finally {
            ResourceUtils.closeAll();
        }
    }
}
