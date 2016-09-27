package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashSet;

public class HostIdentifierCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      getNodesViaNic(context);
      return true;
   }

   public void getNodesViaNic(DiagnosticContext context) {

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

      context.setHostIpList(ipAndHosts);
      logger.debug("IP and Hostname list:" + ipAndHosts);
   }


   public String getHostName() {
      String s = null;
      try {

         Process p = Runtime.getRuntime().exec("hostname");

         BufferedReader stdInput = new BufferedReader(new
            InputStreamReader(p.getInputStream()));

         BufferedReader stdError = new BufferedReader(new
            InputStreamReader(p.getErrorStream()));
         s = stdInput.readLine();

      } catch (IOException e) {
         logger.error("Error retrieving hostname.", e);
      }

      return s;
   }


}
