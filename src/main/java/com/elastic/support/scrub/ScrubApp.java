package com.elastic.support.scrub;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagnosticInputs;
import com.elastic.support.diagnostics.ShowHelpException;
import com.elastic.support.monitoring.MonitoringImportInputs;
import com.elastic.support.monitoring.MonitoringImportService;
import com.elastic.support.util.ResourceCache;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;


public class ScrubApp {

    private static Logger logger = LogManager.getLogger(ScrubApp.class);

    public static void main(String[] args) {

        try {
            ScrubInputs scrubInputs = new ScrubInputs();
            if (args.length == 0) {
                logger.error(Constants.CONSOLE,  Constants.interactiveMsg);
                scrubInputs.interactive = true;
                scrubInputs.runInteractive();
            } else {
                List<String> errors = scrubInputs.parseIinputs(args);
                if (errors.size() > 0) {
                    for (String err : errors) {
                        logger.error(Constants.CONSOLE,  err);
                    }
                    scrubInputs.usage();
                    SystemUtils.quitApp();
                }
            }
            ResourceCache.terminal.dispose();
            new ScrubService().exec(scrubInputs);
        } catch (ShowHelpException she){
            SystemUtils.quitApp();
        } catch (Exception e) {
            logger.error(Constants.CONSOLE,  "Fatal error occurred: {}. {}", e.getMessage(), Constants.CHECK_LOG);
        } finally {
            ResourceCache.closeAll();
        }
    }

}