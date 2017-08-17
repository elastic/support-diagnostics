package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.JsonYamlUtils;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;

public class HostIdentifierCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      try {
         String temp = context.getTempDir();
         int port = context.getInputParams().getPort();
         JsonNode rootNode = JsonYamlUtils.createJsonNodeFromFileName(temp, Constants.NODES);
         JsonNode nodes = rootNode.path("nodes");
         Iterator<JsonNode> it = nodes.iterator();
         HashSet hosts = getNodesViaNic(context);
         context.setHostIpList(hosts);

         while (it.hasNext()) {
            JsonNode n = it.next();
            String host = n.path("host").asText();
            String ip = n.path("ip").asText();

            // if the host we're on doesn't match up with the node entry
            // then bypass it and move to the next node
            if (hosts.contains(host) || hosts.contains(ip)) {
               JsonNode settings = n.path("settings");
               if (port != 9200) {
                  // There may be more than one node on this host
                  // but we only want the one that it's run against
                  JsonNode httpSettings = settings.path("http");
                  int httpPort = httpSettings.path("port").asInt();
                  if (port != httpPort) {
                     continue;
                  }
               }
               context.setAttribute("diagNode", n);
               JsonNode jnode = n.path("process");
               String pid = jnode.path("id").asText();
               context.setPid(pid);
               break;
            }
         }
      } catch (Exception e) {
         logger.error("Error identifying host of diag node.", e);
      }

      return true;
   }

   public HashSet getNodesViaNic(DiagnosticContext context) {

      // Check system for NIC's to get ip's and hostnames
      HashSet ipAndHosts = new HashSet();

      // Add the node address they passed in.
      ipAndHosts.add(context.getInputParams().getHost());

      logger.info("Retrieving network interface information.");
      try {
         // Get the system hostname and add it.
         String hostName = getHostName();
         context.setAttribute("hostName", hostName);
         ipAndHosts.add(hostName);
         Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();

         while (nics.hasMoreElements()) {
            NetworkInterface nic = nics.nextElement();
            ipAndHosts.add(nic.getDisplayName());
            Enumeration<InetAddress> inets = nic.getInetAddresses();

            while (inets.hasMoreElements()) {
               InetAddress inet = inets.nextElement();
               ipAndHosts.add(inet.getHostAddress());
               ipAndHosts.add(inet.getHostName());
               ipAndHosts.add(inet.getCanonicalHostName());
            }
         }
      } catch (Exception e) {
         logger.error("Error occurred acquiring IP's and hostnames", e);
      }

      logger.debug("IP and Hostname list:" + ipAndHosts);

      return ipAndHosts;
   }


   public String getHostName() {
      String s = null;
      try {

         Process p = Runtime.getRuntime().exec("hostname");

         BufferedReader stdInput = new BufferedReader(new
            InputStreamReader(p.getInputStream()));

         s = stdInput.readLine();

      } catch (IOException e) {
         logger.error("Error retrieving hostname.", e);
      }

      return s;
   }


}
