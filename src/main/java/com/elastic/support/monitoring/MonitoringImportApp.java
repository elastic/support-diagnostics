package com.elastic.support.monitoring;

import com.elastic.support.BaseApp;
import com.elastic.support.Constants;
import com.elastic.support.diagnostics.ShowHelpException;
import com.elastic.support.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class MonitoringImportApp extends BaseApp {


    private static final Logger logger = LogManager.getLogger(com.elastic.support.monitoring.MonitoringImportApp.class);

    public static void main(String[] args) {

        try {
            Map configMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
            MonitoringImportConfig config = new MonitoringImportConfig(configMap);
            MonitoringImportInputs inputs = new MonitoringImportInputs(config.delimiter);
            initInputs(args, inputs);
            ResourceUtils.startLog(inputs.outputDir + SystemProperties.fileSeparator + "diagnostic.log");
            elasticsearchConnection(inputs, config);
            githubConnection(config);
            new MonitoringImportService(inputs, config).exec();
            ResourceUtils.closeFileLogs();
            SystemUtils.nukeDirectory(inputs.tempDir);
        } catch (ShowHelpException she){
            SystemUtils.quitApp();
        } catch (Exception e) {
            logger.error(Constants.CONSOLE,  "Error occurred: {}. {}", e.getMessage(), Constants.CHECK_LOG);
        } finally {
            ResourceUtils.closeAll();
        }
    }

}
