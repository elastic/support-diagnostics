package com.elastic.support.diagnostics;

import com.elastic.support.BaseService;
import com.elastic.support.Constants;
import com.elastic.support.diagnostics.chain.DiagnosticChainExec;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiagnosticService implements BaseService {

    private Logger logger = LogManager.getLogger(DiagnosticService.class);

    DiagnosticInputs inputs;
    DiagnosticConfig config;

    public DiagnosticService(DiagnosticInputs inputs, DiagnosticConfig config){
        this.config = config;
        this.inputs = inputs;
    }

    public void exec() {
        DiagnosticContext ctx = new DiagnosticContext();
        ctx.diagsConfig = config;
        ctx.diagnosticInputs = inputs;
       try{
            DiagnosticChainExec.runDiagnostic(ctx, inputs.diagType);
            if (ctx.dockerPresent) {
                logger.info(Constants.CONSOLE, "Identified Docker installations - bypassed log collection and some system calls.");
            }
        } catch (DiagnosticException de) {
            logger.error(Constants.CONSOLE, de.getMessage());
        }
    }
}
