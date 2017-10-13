package com.elastic.support.diagnostics.commands;

import com.elastic.support.util.SystemProperties;
import com.elastic.support.diagnostics.chain.DiagnosticContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

public class SystemCallsCmd extends BaseSystemCallsCmd {

   public boolean execute(DiagnosticContext context) {

      if(! context.isProcessLocal()){
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
