package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.InputParams;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.RestExec;
import com.elastic.support.util.SystemProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class GetNodesInfoCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      boolean rc = true;
      RestExec restExec = context.getRestExec();
      InputParams inputs = context.getInputParams();
      String diagNode = context.getDiagNode();
      if(StringUtils.isEmpty(diagNode)){
         return rc;
      }

      logger.info("Trying REST Endpoint for node logging info.");

      try {
         String fileName = context.getTempDir() + SystemProperties.fileSeparator + "nodes.json";
         //restExec.execConfiguredQuery("/_nodes?pretty", "nodes", fileName);

         String logDir = null;
         String nodeResult = restExec.execBasic(inputs.getProtocol() + "://" + inputs.getHost() + ":" + inputs.getPort() + "/_nodes?pretty");
         JsonNode rootNode = JsonYamlUtils.createJsonNodeFromString(nodeResult);

         JsonNode nodes = rootNode.path("nodes");
         List<JsonNode> nodeList = new ArrayList();
         Iterator<JsonNode> it = nodes.iterator();
         while (it.hasNext()){
            nodeList.add(it.next());
         }

        for(JsonNode node: nodeList ){
            String nodeName = node.path("name").asText();
            if(nodeName.equals(diagNode)){
               logDir = node.path("settings").path("path").path("logs").asText();
               break;
            }
         }

         String result = restExec.execBasic(inputs.getProtocol() + "://" + inputs.getHost() + ":" + inputs.getPort());
         JsonNode root = JsonYamlUtils.createJsonNodeFromString(result);
         String clusterName = root.path("cluster_name").asText();
         context.setClusterName(clusterName);

      } catch (Exception e) {
         logger.error("Error retrieving Elasticsearch version  - unable to continue..  Please make sure the proper connection parameters were specified", e);
         rc = false;
      }

      return rc;
   }


}
