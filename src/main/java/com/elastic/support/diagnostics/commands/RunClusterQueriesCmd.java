package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;

import java.util.Map;
import java.util.Set;

public class RunClusterQueriesCmd extends AbstractQueryCmd {

   public boolean execute(DiagnosticContext context) {
      Map<String, String> statements = null;
      String majorVersion = "";

      if(context.getInputParams().isHotThreads()){
         statements = (Map<String, String>) context.getConfig().get("hotThreads");
      }
      else{
         majorVersion = context.getVersion().split("\\.")[0];
         statements = (Map<String, String>) context.getConfig().get("restQueries-" + majorVersion);
      }

      if (statements == null) {
         throw new IllegalArgumentException("major version [" + majorVersion + "] is not supported by this diagnostics tool");
      }
      Set<Map.Entry<String, String>> entries = statements.entrySet();

      logger.debug("Generating full diagnostic.");

      for (Map.Entry<String, String> entry : entries) {
         runQuery(entry, context);
      }

      return true;
   }


}
