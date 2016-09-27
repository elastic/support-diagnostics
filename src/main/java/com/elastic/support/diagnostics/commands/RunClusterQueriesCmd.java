package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;

import java.util.Map;
import java.util.Set;

public class RunClusterQueriesCmd extends AbstractQueryCmd {

   public boolean execute(DiagnosticContext context) {

      String majorVersion = context.getVersion().substring(0, 1);
      Map<String, String> statements = (Map<String, String>) context.getConfig().get("restQueries-" + majorVersion);
      Set<Map.Entry<String, String>> entries = statements.entrySet();

      logger.debug("Generating full diagnostic.");

      for (Map.Entry<String, String> entry : entries) {
         runQuery(entry, context);
      }

      return true;
   }


}
