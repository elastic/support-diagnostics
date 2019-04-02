package com.elastic.support.diagnostics;

import com.elastic.support.config.Constants;
import com.elastic.support.config.DiagConfig;
import com.elastic.support.config.DiagnosticInputs;
import com.elastic.support.util.JsonYamlUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class DiagnosticApp {

    private static final Logger logger = LogManager.getLogger(DiagnosticApp.class);

    public static void main(String[] args) {

        DiagnosticInputs diagnosticInputs = new DiagnosticInputs();
        diagnosticInputs.parseInputs(args);
        if (!diagnosticInputs.validate()) {
            logger.info("Exiting...");
            System.exit(0);
        }

        Map diagMap =JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
        DiagConfig diagConfig = new DiagConfig(diagMap);
        Map<String, List<String>> chains = JsonYamlUtils.readYamlFromClasspath(Constants.CHAINS_CONFIG, true);

        DiagnosticService diag = new DiagnosticService();
        diag.exec(diagnosticInputs, diagConfig, chains);
    }

}

