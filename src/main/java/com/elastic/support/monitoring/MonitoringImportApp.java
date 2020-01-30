package com.elastic.support.monitoring;

import com.elastic.support.Constants;
import com.elastic.support.util.ResourceCache;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class MonitoringImportApp {


    private static final Logger logger = LogManager.getLogger(com.elastic.support.monitoring.MonitoringImportApp.class);

    public static void main(String[] args) {

        try {
            MonitoringImportInputs monitoringImportInputs = new MonitoringImportInputs();
            if (args.length == 0) {
                logger.info("{}{}{}",
                        "Command line options can be displayed with the --help arguemnt.",
                        SystemProperties.lineSeparator,
                        "Entering interactive mode.");
                monitoringImportInputs.interactive = true;
                monitoringImportInputs.runInteractive();
            } else {
                List<String> errors = monitoringImportInputs.parseInputs(args);
                if (errors.size() > 0) {
                    for (String err : errors) {
                        logger.info(err);
                    }
                    monitoringImportInputs.usage();
                    logger.info("Exiting...");
                    System.exit(0);
                }
            }
            ResourceCache.terminal.dispose();
            new MonitoringImportService().execImport(monitoringImportInputs);
        } catch (Exception e) {
            logger.info("Error occurred: {}. {}", e.getMessage(), Constants.CHECK_LOG);
        } finally {
            ResourceCache.closeAll();
        }
    }

}
