package com.elastic.support.diagnostics.chain;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.util.JsonYamlUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class DiagnosticChainExec {

   private static Logger logger = LogManager.getLogger(DiagnosticChainExec.class);

   public void runDiagnostic(DiagnosticContext context) {

      try {
         Map<String, Object> diags = JsonYamlUtils.readYamlFromClasspath("diags.yml", true);
         if (diags.size() == 0) {
            logger.error("Required config file diags.yml was not found. Exiting application.");
            throw new RuntimeException("Missing diags.yml");
         }

         context.setConfig(diags);

         Map<String, Object> chains = JsonYamlUtils.readYamlFromClasspath("chains.yml", false);
         if (chains.size() == 0) {
            logger.error("Required config file chains.yml was not found. Exiting application.");
            throw new RuntimeException("Missing chain.yml");
         }

         String diagType = context.getInputParams().getDiagType();

         List<String> chain = (List) chains.get(diagType);

         if (diagType.equals(Constants.LOGSTASH_DIAG)) {
            context.setDiagName(Constants.LOGSTASH_DIAG + "-" + Constants.ES_DIAG);
         }

         Chain diagnostic = new Chain(chain);
         diagnostic.execute(context);
      } catch (Exception e) {
         logger.error("Error encountered running diagnostic. See logs for additional information.  Exiting application.", e);
         throw new RuntimeException("Diagnostic runtime error", e);
      }

   }

}