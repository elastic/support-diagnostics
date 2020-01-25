package com.elastic.support.monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class MonitoringImportApp {


    private static final Logger logger = LogManager.getLogger(com.elastic.support.monitoring.MonitoringImportApp.class);

    public static void main(String[] args) {

        MonitoringImportInputs monitoringImportInputs = new MonitoringImportInputs();
        List<String> errors = monitoringImportInputs.parseInputs(args);

        if(args.length == 0){
            monitoringImportInputs.interactive = true;
        }
        if( args.length == 0 || monitoringImportInputs.interactive){
            // Create a new input object so we out clean since
            // parameters other than interactive might have been sent in.
            monitoringImportInputs = new MonitoringImportInputs();
            monitoringImportInputs.interactive = true;
            monitoringImportInputs.runInteractive();
        }
        else {
            if (errors.size() > 0) {
                for(String err: errors){
                    logger.info(err);
                }
                monitoringImportInputs.usage();
                logger.info("Exiting...");
                System.exit(0);
            }
        }
        new MonitoringImportService().execImport(monitoringImportInputs);
    }


}
