package com.elastic.support.monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class MonitoringImportApp {


    private static final Logger logger = LogManager.getLogger(com.elastic.support.monitoring.MonitoringImportApp.class);

    public static void main(String[] args) {

        MonitoringImportInputs monitoringImportInputs = new MonitoringImportInputs();
        monitoringImportInputs.parseInputs(args);

        List<String> errors = monitoringImportInputs.validate();
        if(errors.size() > 0){
            for(String err: errors){
                logger.warn(err);
            }
            logger.info("Exiting...");
            System.exit(0);
        }

        new MonitoringImportService().execImport(monitoringImportInputs);
    }


}
