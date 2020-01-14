package com.elastic.support.diagnostics;

import com.elastic.support.Constants;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.ResourceCache;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class DiagnosticApp {

    private static final Logger logger = LogManager.getLogger(DiagnosticApp.class);

    public static void main(String[] args) {

        try {
            DiagnosticInputs diagnosticInputs = new DiagnosticInputs();
            diagnosticInputs.parseInputs(args);
            if(diagnosticInputs.interactive){
                // Start out clean
                diagnosticInputs = new DiagnosticInputs();
                diagnosticInputs.interactive = true;
                diagnosticInputs.runInteractive();
            }
            else {
                List<String> errors = diagnosticInputs.validate();
                if (errors.size() > 0) {
                    for(String err: errors){
                        logger.info(err);
                    }
                    logger.info("Exiting...");
                    System.exit(0);
                }
            }

            Map diagMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);

            DiagConfig diagConfig = new DiagConfig(diagMap);

            DiagnosticService diag = new DiagnosticService();
            diag.exec(diagnosticInputs, diagConfig);
        } catch (Throwable t) {
            logger.error("Unanticipated error", t);
        }
        finally {
        }
    }

}

