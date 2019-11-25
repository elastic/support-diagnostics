package com.elastic.support.diagnostics;

import com.elastic.support.Constants;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class DiagnosticApp {

    private static final Logger logger = LogManager.getLogger(DiagnosticApp.class);

    public static void main(String[] args) {


        try {
            DiagnosticInputs diagnosticInputs = new DiagnosticInputs();
            diagnosticInputs.parseInputs(args);
            if (!diagnosticInputs.validate()) {
                logger.info("Exiting...");
                System.exit(0);
            }

            Map diagMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);

            DiagConfig diagConfig = new DiagConfig(diagMap);

            DiagnosticService diag = new DiagnosticService();
            diag.exec(diagnosticInputs, diagConfig);
        } catch (Throwable t) {
            logger.log(SystemProperties.DIAG, "Unanticipated error", t);
        }
    }

}

