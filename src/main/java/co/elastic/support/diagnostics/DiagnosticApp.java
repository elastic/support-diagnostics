/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics;

import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.util.JsonYamlUtils;
import co.elastic.support.util.ResourceCache;
import co.elastic.support.util.SystemUtils;
import co.elastic.support.Constants;
import co.elastic.support.util.TextIOManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class DiagnosticApp {

    private static final Logger logger = LogManager.getLogger(DiagnosticApp.class);

    public static void main(String[] args) {
        try(
            ResourceCache resourceCache = new ResourceCache();
            TextIOManager textIOManager = new TextIOManager();
        ) {
            DiagnosticInputs diagnosticInputs = new DiagnosticInputs("cli");
            if (args.length == 0) {
                logger.info(Constants.CONSOLE, Constants.interactiveMsg);
                diagnosticInputs.interactive = true;
                diagnosticInputs.runInteractive(textIOManager);
            } else {
                List<String> errors = diagnosticInputs.parseInputs(textIOManager, args);
                if (errors.size() > 0) {
                    for (String err : errors) {
                        logger.error(Constants.CONSOLE, err);
                    }
                    diagnosticInputs.usage();
                    SystemUtils.quitApp();
                }
            }

            Map diagMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
            DiagConfig diagConfig = new DiagConfig(diagMap);
            DiagnosticService diag = new DiagnosticService();
            DiagnosticContext context = new DiagnosticContext(diagConfig, diagnosticInputs, resourceCache, true);

            diag.exec(context);
        } catch (ShowHelpException she){
            SystemUtils.quitApp();
        } catch (Exception e) {
            logger.error(Constants.CONSOLE,"Fatal error occurred: {}. {}", e.getMessage(), Constants.CHECK_LOG);
            logger.error( e);
        }
    }



}
