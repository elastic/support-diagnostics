package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.Constants;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;


public class HostIdentifierCmd  implements Command {

   /**
    * In this command the list of network interfaces is obtained from the Java runtime
    * and compared against the addresses in the nodes output to determine which node
    * the diagnostic is running on. Note that this can be fairly slow on some OS's
    * particularly OSX on Macbooks.
    */
   private final Logger logger = LogManager.getLogger(HostIdentifierCmd.class);

   public void execute(DiagnosticContext context) {

      try {
         logger.info("Checking the supplied hostname against the node information retrieved to verify location. This may take some time...");
         String version = context.getVersion();
         String temp = context.getTempDir();
         String targetHost = context.getDiagnosticInputs().getHost();

         String systemDigest = context.getSystemDigest();
         Set<String> localAddr = SystemUtils.getNetworkInterfaces();

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

               String host = node.path("settings").path("network").path("host").asText();
               addToHostList(host, nodeAddr);

               host = node.path("host").asText();
               addToHostList(host, nodeAddr);

               host = node.path("ip").asText();
               addToHostList(host, nodeAddr);

               String name = node.path("name").asText();

               if (version.startsWith("1.")) {
                  String publishAddress = node.path("http").path("publish_address").asText();
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
            String pid = targetNode.path("process").path("id").asText();
            context.setPid(pid);
            String nodeName = targetNode.path("name").asText();
            String logDir = targetNode.path("settings").path("path").path("logs").asText();
            context.setLogDir(logDir);
         }

      } catch (Exception e) {
         context.setPid(Constants.NOT_FOUND);
         context.setLogDir(Constants.NOT_FOUND);
         logger.log(SystemProperties.DIAG, "Error identifying host of diag node.", e);
      }

   }

   private void addToHostList(String host, Set hostList){
      if (! StringUtils.isEmpty(host)){
         hostList.add(host);
      }
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
