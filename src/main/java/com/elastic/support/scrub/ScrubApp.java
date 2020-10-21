package com.elastic.support.scrub;

import com.elastic.support.BaseApp;
import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagnosticConfig;
import com.elastic.support.diagnostics.ShowHelpException;
import com.elastic.support.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;


public class ScrubApp extends BaseApp {

    private static Logger logger = LogManager.getLogger(ScrubApp.class);

    public static void main(String[] args) {

        ScrubInputs inputs = null;
        try {
            Map configMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
            DiagnosticConfig config = new DiagnosticConfig(configMap);
            inputs = new ScrubInputs(config.delimiter);
            initInputs(args, inputs);
            ResourceUtils.startLog(inputs.tempDir + SystemProperties.fileSeparator + "diagnostic.log");
            new ScrubService(inputs).exec();
            ArchiveUtils.archiveDirectory(inputs.tempDir, inputs.outputDir + SystemProperties.fileSeparator + "scrubbed-" + inputs.scrubbedFileBaseName +  ".zip");
        } catch (ShowHelpException she){
            inputs.usage();
            SystemUtils.quitApp();
        } catch (Exception e) {
            logger.error(Constants.CONSOLE,  "Fatal error occurred: {}. {}", e.getMessage(), Constants.CHECK_LOG);
        } finally {

            ResourceUtils.closeAll();
        }
    }

}