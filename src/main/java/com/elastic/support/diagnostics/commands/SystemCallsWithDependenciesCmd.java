package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

public class ProcessBasedSystemCallsCmd extends BaseSystemCallsCmd {

   public boolean execute(DiagnosticContext context) {

      if(! context.isLocalAddressLocated() || ! context.isDiagNodeFound()){
         logger.warn("The diagnostic does not appear to be running on a host that contains a running node or that node could not be located in the retrieved list from the cluster.");
         logger.warn("No system calls will be run. Utility should probably be run with --type remote.");
         return true;
      }

      String pid = context.getPid();
      String javaHome = SystemProperties.javaHome;

      if( !isJdkPresent() || !isProcessPresent(pid)) {
         logger.warn("Either JDK or Process Id was not present - bypassing those checks");
         return true;
      }

      try {
         String os = checkOS() + "-dep";
         Map<String, String> osCmds = (Map<String, String>) context.getConfig().get(os);
         osCmds = processArgs(osCmds, pid, javaHome);
        processCalls(context, osCmds);
      } catch (Exception e) {
         logger.error("Error executing one or more system calls.", e);
      }

      return true;

   }
   
}

