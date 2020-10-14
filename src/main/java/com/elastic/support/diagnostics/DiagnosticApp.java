package com.elastic.support.diagnostics;

import com.elastic.support.*;
import com.elastic.support.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiagnosticApp extends BaseApp {

    private static final Logger logger = LogManager.getLogger(DiagnosticApp.class);

    public static void main(String[] args) {

        try {
            DiagnosticConfig config = new DiagnosticConfig(JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true));
            DiagnosticInputs inputs = new DiagnosticInputs(config.delimiter);
            initInputs(args, inputs);
            elasticsearchConnection(inputs, config);
            githubConnection(config);
            DiagnosticService service = new DiagnosticService(inputs, config);
            runServiceSequence(inputs, config, service, inputs.diagType);
        } catch (ShowHelpException she){
            SystemUtils.quitApp();
        } catch (Exception e) {
            logger.error(Constants.CONSOLE,"Fatal error occurred: {}. {}", e.getMessage(), Constants.CHECK_LOG);
            logger.error( e);
        } finally {
            ResourceUtils.closeAll();
        }
    }

    protected static void runServiceSequence(BaseInputs inputs, BaseConfig config, BaseService service, String archiveType){
        try {
            inputs.tempDir = ResourceUtils.createTempDirectory(inputs.outputDir);
            ResourceUtils.startLog(inputs.tempDir + SystemProperties.fileSeparator + "diagnostic.log");
            service.exec();
        } catch (Exception e) {
            System.out.println("Error during service run. Check logs in temp directory or last archive created.");
        } finally {
            ResourceUtils.closeFileLogs();
            ArchiveUtils.archiveDirectory(inputs.tempDir, inputs.outputDir + SystemProperties.fileSeparator + archiveType + "-diagnostics-" + SystemProperties.getFileDateString() + ".zip");
            SystemUtils.nukeDirectory(inputs.tempDir);
        }
    }
}

