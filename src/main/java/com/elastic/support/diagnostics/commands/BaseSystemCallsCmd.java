package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

public abstract class BaseSystemCallsCmd extends AbstractDiagnosticCmd {

   public void executeCalls(Map<String, String> calls, DiagnosticContext context) {

      ProcessBuilder pb = new ProcessBuilder();
      pb.redirectErrorStream(true);

      Iterator<Map.Entry<String, String>> iter = calls.entrySet().iterator();
      final List<String> cmds = new ArrayList<>();

      while (iter.hasNext()) {
         Map.Entry<String, String> entry =  iter.next();
         String cmdLabel = entry.getKey();
         String cmdText = entry.getValue();
         if (cmdText.contains("PID")) {
            String pid = context.getPid();
            if (pid.equals("0")) {
               logger.error("Process id for node local to diagnostic not found. Bypassing:" + entry.getKey() + ", " + cmdText);
            } else {
               cmdText = entry.getValue().replace("PID", pid);
            }
         }

         if (cmdText.contains("JAVA_HOME")) {
            String javaHome = SystemProperties.javaHome;
               cmdText = entry.getValue().replace("JAVA_HOME", javaHome);
         }

         try {
            StringTokenizer st = new StringTokenizer(cmdText, " ");
            while (st.hasMoreTokens()) {
               cmds.add(st.nextToken());
            }
            logger.info("Running: " + cmdText);
            pb.redirectOutput(new File(context.getTempDir() + SystemProperties.fileSeparator + cmdLabel + ".txt"));
            pb.command(cmds);
            Process pr = pb.start();
            pr.waitFor();
         } catch (Exception e) {
            logger.error("Error processing system command:" + cmdLabel);
            try{
               FileOutputStream fos = new FileOutputStream(new File(context.getTempDir() + SystemProperties.fileSeparator + cmdLabel + ".txt"), true);
               PrintStream ps = new PrintStream(fos);
               e.printStackTrace(ps);
            }
            catch (Exception ie){
               logger.error("Error processing system command", ie);
            }
         } finally {
            cmds.clear();
         }
      }
   }

}
