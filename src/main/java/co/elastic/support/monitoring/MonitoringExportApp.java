/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.monitoring;

import co.elastic.support.util.ResourceCache;
import co.elastic.support.Constants;
import co.elastic.support.diagnostics.ShowHelpException;
import co.elastic.support.util.SystemUtils;
import co.elastic.support.util.TextIOManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class MonitoringExportApp {

    private static final Logger logger = LogManager.getLogger(MonitoringExportApp.class);

    public static void main(String[] args) {

        ResourceCache resourceCache = new ResourceCache();
        TextIOManager textIOManager = new TextIOManager();
        try {
            MonitoringExportInputs monitoringExportInputs = new MonitoringExportInputs();
            if (args.length == 0) {
                logger.info(Constants.CONSOLE,  Constants.interactiveMsg);
                monitoringExportInputs.interactive = true;
                monitoringExportInputs.runInteractive(textIOManager);
            } else {
                List<String> errors = monitoringExportInputs.parseInputs(args);
                if (errors.size() > 0) {
                    for (String err : errors) {
                        logger.error(Constants.CONSOLE, err);
                    }
                    monitoringExportInputs.usage();
                    SystemUtils.quitApp();
                }
            }
            new MonitoringExportService().execExtract(monitoringExportInputs);
        } catch (ShowHelpException she){
            SystemUtils.quitApp();
        } catch (Exception e) {
            logger.error(Constants.CONSOLE, "Fatal error occurred: {}. {}", e.getMessage(), Constants.CHECK_LOG);
        } finally {
            resourceCache.closeAll();
            textIOManager.close();
        }
    }
}
