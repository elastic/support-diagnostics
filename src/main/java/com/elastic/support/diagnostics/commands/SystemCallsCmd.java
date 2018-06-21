package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;

import java.util.Map;

public class SystemCallsCmd extends BaseSystemCallsCmd {

   public boolean execute(DiagnosticContext context) {

      try {
         if(context.getInputParams().isNoSystemCalls()){
            logger.info("Bypassing system calls...");
            return true;
         }
         String os = checkOS();
         Map<String, String> osCmds = (Map<String, String>) context.getConfig().get(os);
         processCalls(context, osCmds);
      } catch (Exception e) {
         logger.error("Error executing one or more system calls.", e);
      }
      return true;
   }

}
