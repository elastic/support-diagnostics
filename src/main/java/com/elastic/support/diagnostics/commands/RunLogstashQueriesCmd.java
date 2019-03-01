package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.JsonYamlUtils;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;

public class RunLogstashQueriesCmd extends BaseQueryCmd {

   public void execute(DiagnosticContext context) {

      Map<String, String> statements = (Map<String, String>) GlobalContext.getConfig().get("logstash");
      Set<Map.Entry<String, String>> entries = statements.entrySet();

      logger.debug("Generating full diagnostic.");

      for (Map.Entry<String, String> entry : entries) {
         runQuery(entry, context);
      }

      try {
         String temp = context.getTempDir();
         JsonNode nodeData = JsonYamlUtils.createJsonNodeFromFileName(temp, "logstash_node.json");
         JsonNode jvm = nodeData.path("jvm");
         String pid = jvm.path("pid").asText();
         context.setPid(pid);

      } catch (Exception e) {
         logger.error("Error obtaining logstash process id", e);
      }

   }


}
