package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.StringUtils;
import java.util.*;


public class HostIdentifierCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      try {
         logger.info("Checking the supplied hostname against the node information retrieved to verify location. This may take some time...");
         // If we're doing multiple runs we don't need to do this again.
         if (context.getCurrentRep() > 1) {
            return true;
         }

         String version = context.getVersion();
         String temp = context.getTempDir();
         String targetHost = context.getInputParams().getHost();

         String systemDigest = context.getStringAttribute("systemDigest");
         HashSet<String> localAddr = parseNetworkAddresses(systemDigest);

         JsonNode rootNode = JsonYamlUtils.createJsonNodeFromFileName(temp, Constants.NODES);
         JsonNode nodes = rootNode.path("nodes");
         List<JsonNode> nodeList = new ArrayList();
         Iterator<JsonNode> it = nodes.iterator();
         while (it.hasNext()) {
            nodeList.add(it.next());
         }

         int nbrNodes = nodeList.size();

         JsonNode targetNode = null;

         if (nbrNodes == 1){
            targetNode = nodeList.get(0);
         } else {
            for (JsonNode node : nodeList) {
               Set<String> nodeAddr = new HashSet<>();

               String networkHost = SystemUtils.toString(node.path("settings").path("network").path("host").asText(), "");
               if (!StringUtils.isEmpty(networkHost)) {
                  nodeAddr.add(networkHost);
               }
               String host = SystemUtils.toString(node.path("host").asText(), "");
               if (!StringUtils.isEmpty(host)) {
                  nodeAddr.add(host);
               }
               String ip = SystemUtils.toString(node.path("ip").asText(), "");
               if (!StringUtils.isEmpty(host)) {
                  nodeAddr.add(ip);
               }

               String name = SystemUtils.toString(node.path("name").asText(), "");

               if (version.startsWith("1.")) {
                  String publishAddress = SystemUtils.toString(node.path("http").path("publish_address").asText(), "/:");
                  int idxa = publishAddress.indexOf("/");
                  int idxb = publishAddress.indexOf(":");
                  publishAddress = publishAddress.substring(idxa, idxb - 1);
                  if (!"".equals(publishAddress)) {
                     nodeAddr.add(publishAddress);
                  }
               } else {
                  JsonNode bnds = node.path("http").path("bound_address");
                  if (bnds instanceof ArrayNode) {
                     ArrayNode boundAddresses = (ArrayNode) bnds;
                     for (JsonNode bnd : boundAddresses) {
                        String addr = bnd.asText();
                        addr = addr.substring(0, addr.indexOf(":"));
                        nodeAddr.add(addr);
                     }
                  }
               }

               if (isLocalNode(localAddr, nodeAddr )){
                  targetNode = node;
                  break;
               }
            }
         }

         if (targetNode == null) {
            logger.log(SystemProperties.DIAG, "{} host was not found in the nodes output", targetHost);
            throw new RuntimeException("Could not determine which node is installed on this host. Bypassing system calls and log collection");
         } else {
            String pid = SystemUtils.toString(targetNode.path("process").path("id").asText(), Constants.NOT_FOUND);
            context.setPid(pid);
            String nodeName = SystemUtils.toString(targetNode.path("name").asText(), Constants.NOT_FOUND);
            context.setDiagName(nodeName);
            String logDir = SystemUtils.toString(targetNode.path("settings").path("path").path("logs").asText(), Constants.NOT_FOUND);
            context.setLogDir(logDir);
            String elasticHome = SystemUtils.toString(targetNode.path("settings").path("path").path("home").asText(), Constants.NOT_FOUND);
            context.setEsHome(elasticHome);

         }

      } catch (Exception e) {
         context.setPid(Constants.NOT_FOUND);
         context.setDiagName(Constants.NOT_FOUND);
         context.setLogDir(Constants.NOT_FOUND);
         context.setEsHome(Constants.NOT_FOUND);
         logger.log(SystemProperties.DIAG, "Error identifying host of diag node.", e);
      }

      return true;
   }

   private boolean isLocalNode(Set<String> localAddr, Set<String>nodeAddr){

      for(String addr: localAddr){
         if(nodeAddr.contains(addr)){
            return true;
         }
      }

      return false;
   }

   private HashSet<String> parseNetworkAddresses(String systemDigest) throws Exception {

      HashSet<String> ipAndHosts = new HashSet<>();
      JsonNode root = JsonYamlUtils.createJsonNodeFromString(systemDigest);
      Iterator<JsonNode> networks = root.path("hardware")
         .path("networks")
         .iterator();
      while (networks.hasNext()) {
         JsonNode nic = networks.next();
         Iterator<JsonNode> addrs = nic.path("ipv4").iterator();
         while (addrs.hasNext()) {
            String ip = addrs.next().asText();
            ipAndHosts.add(ip);
         }
      }

      return ipAndHosts;

   }

}
