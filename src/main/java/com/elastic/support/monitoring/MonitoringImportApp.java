package com.elastic.support.monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MonitoringImportApp {


    private static final Logger logger = LogManager.getLogger(com.elastic.support.monitoring.MonitoringImportApp.class);

    public static void main(String[] args) {

        MonitoringImportInputs monitoringImportInputs = new MonitoringImportInputs();
        monitoringImportInputs.parseInputs(args);

        if (!monitoringImportInputs.validate()) {
            logger.info("Exiting...");
            System.exit(0);
        }

        MonitoringImportService service = new MonitoringImportService(monitoringImportInputs);
        service.execImport();
    }


}
