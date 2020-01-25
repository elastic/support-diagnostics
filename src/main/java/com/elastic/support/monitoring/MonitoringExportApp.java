package com.elastic.support.monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class MonitoringExportApp {

    private static final Logger logger = LogManager.getLogger(MonitoringExportApp.class);

    public static void main(String[] args) {

        MonitoringExportInputs monitoringExportInputs = new MonitoringExportInputs();
        List<String> errors = monitoringExportInputs.parseInputs(args);

        if(args.length == 0){
            monitoringExportInputs.interactive = true;
        }
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

        new MonitoringExportService().execExtract(monitoringExportInputs);
    }
}
