package com.elastic.support.monitoring;

import com.elastic.support.Constants;
import com.elastic.support.util.ResourceCache;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class MonitoringExportApp {

    private static final Logger logger = LogManager.getLogger(MonitoringExportApp.class);

    public static void main(String[] args) {

        try {
            MonitoringExportInputs monitoringExportInputs = new MonitoringExportInputs();
            if (args.length == 0) {
                logger.info(Constants.interactiveMsg);
                monitoringExportInputs.interactive = true;
                monitoringExportInputs.runInteractive();
            } else {
                List<String> errors = monitoringExportInputs.parseInputs(args);
                if (errors.size() > 0) {
                    for (String err : errors) {
                        logger.info(err);
                    }
                    monitoringExportInputs.usage();
                    logger.info("Exiting...");
                    System.exit(0);
                }
            }
            // Needs to be done for both because in command line it
            // may be used for passwords.
            ResourceCache.terminal.dispose();
            new MonitoringExportService().execExtract(monitoringExportInputs);
        } catch (Exception e) {
            logger.info("Fatal error occurred: {}. {}", e.getMessage(), Constants.CHECK_LOG);
        } finally {
            ResourceCache.closeAll();
        }
    }
}
