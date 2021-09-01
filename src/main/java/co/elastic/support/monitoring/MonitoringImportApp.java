/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.monitoring;

import co.elastic.support.Constants;
import co.elastic.support.diagnostics.ShowHelpException;
import co.elastic.support.util.ResourceCache;
import co.elastic.support.util.SystemUtils;
import co.elastic.support.util.TextIOManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class MonitoringImportApp {


    private static final Logger logger = LogManager.getLogger(MonitoringImportApp.class);

    public static void main(String[] args) {

        ResourceCache resourceCache = new ResourceCache();
        TextIOManager textIOManager = new TextIOManager();

        try {
            MonitoringImportInputs monitoringImportInputs = new MonitoringImportInputs();
            if (args.length == 0) {
                logger.info(Constants.CONSOLE,  Constants.interactiveMsg);
                monitoringImportInputs.interactive = true;
                monitoringImportInputs.runInteractive(textIOManager);
            } else {
                List<String> errors = monitoringImportInputs.parseInputs(args);
                if (errors.size() > 0) {
                    for (String err : errors) {
                        logger.error(Constants.CONSOLE,  err);
                    }
                    monitoringImportInputs.usage();
                    SystemUtils.quitApp();
                }
            }
            new MonitoringImportService().execImport(monitoringImportInputs);
        } catch (ShowHelpException she){
            SystemUtils.quitApp();
        } catch (Exception e) {
            logger.error(Constants.CONSOLE,  "Error occurred: {}. {}", e.getMessage(), Constants.CHECK_LOG);
        } finally {
            resourceCache.closeAll();
            textIOManager.close();
        }
    }

}
