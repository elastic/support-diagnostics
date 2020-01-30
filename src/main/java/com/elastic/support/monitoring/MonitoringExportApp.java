package com.elastic.support.monitoring;

import com.elastic.support.Constants;
import com.elastic.support.util.ResourceCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class MonitoringExportApp {

    private static final Logger logger = LogManager.getLogger(MonitoringExportApp.class);

    public static void main(String[] args) {

        try {
            MonitoringExportInputs monitoringExportInputs = new MonitoringExportInputs();
            if(args.length == 0){
                monitoringExportInputs.interactive = true;
            }
            List<String> errors = monitoringExportInputs.parseInputs(args);

            if( args.length == 0 || monitoringExportInputs.interactive){
                // Create a new input object so we out clean since
                // parameters other than interactive might have been sent in.
                monitoringExportInputs = new MonitoringExportInputs();
                monitoringExportInputs.interactive = true;
                monitoringExportInputs.runInteractive();
            }
            else {
                if (errors.size() > 0) {
                    for(String err: errors){
                        logger.info(err);
                    }
                    monitoringExportInputs.usage();
                    logger.info("Exiting...");
                    System.exit(0);
                }
            }
            ResourceCache.terminal.dispose();
            new MonitoringExportService().execExtract(monitoringExportInputs);
        } catch (Exception e) {
            logger.info("Fatal error occurred: {}. {}", e.getMessage(), Constants.CHECK_LOG);
        } finally {
            ResourceCache.closeAll();
        }
    }
}
