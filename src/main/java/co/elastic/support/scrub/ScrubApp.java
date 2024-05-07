/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.scrub;

import co.elastic.support.diagnostics.commands.GenerateManifest;
import co.elastic.support.Constants;
import co.elastic.support.diagnostics.ShowHelpException;
import co.elastic.support.util.ResourceCache;
import co.elastic.support.util.SystemUtils;
import co.elastic.support.util.TextIOManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;


public class ScrubApp {

    private static Logger logger = LogManager.getLogger(ScrubApp.class);

    public static void main(String[] args) {
        try(
            TextIOManager textIOManager = new TextIOManager();
        ) {
            ScrubInputs scrubInputs = new ScrubInputs();
            if (args.length == 0) {
                logger.error(Constants.CONSOLE,  Constants.interactiveMsg);
                scrubInputs.interactive = true;
                scrubInputs.runInteractive(textIOManager);
            } else {
                List<String> errors = scrubInputs.parseInputs(textIOManager, args);
                if (errors.size() > 0) {
                    for (String err : errors) {
                        logger.error(Constants.CONSOLE,  err);
                    }
                    scrubInputs.usage();
                    SystemUtils.quitApp();
                }
            }
            logger.info(Constants.CONSOLE, "Using version: {} of diagnostic-utiliy", GenerateManifest.class.getPackage().getImplementationVersion());
            new ScrubService().exec(scrubInputs);
        } catch (ShowHelpException she){
            SystemUtils.quitApp();
        } catch (Exception e) {
            logger.error(Constants.CONSOLE,  "\nFATAL ERROR occurred: {}. {}", e.getMessage(), Constants.CHECK_LOG);
        }
    }

}
