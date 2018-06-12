package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.diagnostics.InputParams;
import com.elastic.support.util.RestExec;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;


public class VersionAndClusterNameCheckCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      // Get the version number and cluster name fromt the JSON returned
      // by just submitting the host/port combo
      Map resultMap = null;
      InputParams inputs = context.getInputParams();
      boolean rc = true;

      logger.info("Trying REST Endpoint.");

      try {
         RestExec restExec = context.getRestExec();
         String result = restExec.execBasic(inputs.getProtocol() + "://" + inputs.getHost() + ":" + inputs.getPort());
         ObjectMapper mapper = new ObjectMapper();
         resultMap = mapper.readValue(result, LinkedHashMap.class);
         String clusterName = (String) resultMap.get("cluster_name");
         Map ver = (Map) resultMap.get("version");
         String versionNumber = (String) ver.get("number");
         context.setClusterName(clusterName);
         context.setVersion(versionNumber);

      } catch (Exception e) {
         logger.error("Error retrieving Elasticsearch version  - unable to continue..  Please make sure the proper connection parameters were specified", e);
         rc = false;
      }

      return rc;
   }


}
