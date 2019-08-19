package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.Constants;
import com.elastic.support.diagnostics.DiagConfig;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class RunLogstashQueriesCmd extends BaseQueryCmd {

   /**
    * Executes the REST calls for Logstash
    */

   private static final Logger logger = LogManager.getLogger(BaseQueryCmd.class);

   public void execute(DiagnosticContext context) {

      try {
         DiagConfig diagConfig = context.getDiagsConfig();
         Map<String, String> entries = diagConfig.getCommandMap("logstash");
         runQueries(context.getEsRestClient(), entries, context.getTempDir(), diagConfig );
         String temp = context.getTempDir();
         JsonNode nodeData = JsonYamlUtils.createJsonNodeFromFileName(temp, "logstash_node.json");
         JsonNode jvm = nodeData.path("jvm");
         String pid = jvm.path("pid").asText();
         context.setPid(pid);

      } catch (Throwable t) {
         logger.log(SystemProperties.DIAG, "Logstash Query error:", t);
         throw new DiagnosticException(String.format("Error obtaining logstash output and/or process id - exiting. %s", Constants.CHECK_LOG));
      }
   }


}
