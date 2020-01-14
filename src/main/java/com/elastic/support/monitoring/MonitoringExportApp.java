package com.elastic.support.monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class MonitoringExportApp {

    private static final Logger logger = LogManager.getLogger(MonitoringExportApp.class);

    public static void main(String[] args) {

        MonitoringExportInputs monitoringExportInputs = new MonitoringExportInputs();
        monitoringExportInputs.parseInputs(args);

        List<String> errors = monitoringExportInputs.validate();
        if (errors.size() > 0) {
            for(String err: errors){
                logger.info(err);
            }
            logger.info("Exiting...");
            System.exit(0);
        }

        new MonitoringExportService().execExtract(monitoringExportInputs);
    }
}
