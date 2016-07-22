package com.elastic.support.diagnostics.commands;

import com.elastic.support.SystemProperties;
import com.elastic.support.diagnostics.DiagnosticContext;
import com.elastic.support.diagnostics.InputParams;
import com.elastic.support.diagnostics.RestModule;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RunLogstashQueriesCmd extends AbstractQueryCmd {

   public boolean execute(DiagnosticContext context) {

      Map<String, String> statements = (Map<String, String>) context.getConfig().get("logstash");
      Set<Map.Entry<String, String>> entries = statements.entrySet();

      logger.debug("Generating full diagnostic.");

      for (Map.Entry<String, String> entry : entries) {
         runQuery(entry, context);
      }

      return true;
   }


}
