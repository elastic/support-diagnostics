package com.elastic.support.diagnostics.commands;

import com.elastic.support.util.SystemProperties;
import com.elastic.support.diagnostics.chain.DiagnosticContext;

import java.util.*;

public class SystemCallsCmd extends BaseSystemCallsCmd {

   public boolean execute(DiagnosticContext context) {

      if(! context.isLocalAddressLocated() || ! context.isDiagNodeFound()){
         logger.warn("The diagnostic does not appear to be running on a host that contains a running node or that node could not be located in the retrieved list from the cluster.");
         logger.warn("No system calls will be run. Utility should probably be run with --type remote.");
         return true;
      }

      String os = checkOS();
      Map<String, String> osCmds = (Map<String, String>) context.getConfig().get(os);
      executeCalls(osCmds, context);
      return true;
   }

   public String checkOS() {
      String osName = SystemProperties.osName.toLowerCase();
      if (osName.contains("windows")) {
         return "winOS";
      } else if (osName.contains("linux")) {
         return "linuxOS";
      } else if (osName.contains("darwin") || osName.contains("mac os x")) {
         return "macOS";
      } else {
         logger.error("Failed to detect operating system!");
         throw new RuntimeException("Unsupported OS");
      }
   }

}
