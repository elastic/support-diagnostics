package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.InputParams;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.RestExec;
import com.elastic.support.util.SystemProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;


public class GetNodesInfoCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      RestExec restExec = context.getRestExec();
      boolean rc = true;

      logger.info("Trying REST Endpoint.");

      try {
         String fileName = context.getTempDir() + SystemProperties.fileSeparator + "nodes.json";
         restExec.execConfiguredQuery("/_nodes?pretty", "nodes", fileName);

      } catch (Exception e) {
         logger.error("Error retrieving Elasticsearch version  - unable to continue..  Please make sure the proper connection parameters were specified", e);
         rc = false;
      }

      return rc;
   }


}
