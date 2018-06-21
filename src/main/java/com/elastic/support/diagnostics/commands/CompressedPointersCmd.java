package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class CompressedPointersCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      String pid = context.getPid();

      ProcessBuilder pb = new ProcessBuilder();
      pb.redirectErrorStream(true);

      List cmds = new ArrayList();

      try {

         String maxHeap = getMaxHeapSize(pid, new File(context.getTempDir() + SystemProperties.fileSeparator + "jps.txt"));

         String cmdText = "java -Xms1m ### -XX:+UnlockDiagnosticVMOptions -XX:+PrintCompressedOopsMode -version ";
         cmdText = cmdText.replace("###", maxHeap);

         StringTokenizer st = new StringTokenizer(cmdText, " ");
         while (st.hasMoreTokens()) {
            cmds.add(st.nextToken());
         }

         pb.redirectOutput(new File(context.getTempDir() + SystemProperties.fileSeparator + "compressed_ptrs.txt"));
         pb.command(cmds);
         Process pr = pb.start();
         pr.waitFor();
      } catch (Exception e) {
         logger.error("Error processing system command:compressed_ptrs.");
         try {
            FileOutputStream fos = new FileOutputStream(new File(context.getTempDir() + SystemProperties.fileSeparator + "compressed_ptrs.txt"), true);
            PrintStream ps = new PrintStream(fos);
            e.printStackTrace(ps);
         } catch (Exception ie) {
            logger.error("Error processing system command", ie);
         }
      } finally {
         cmds.clear();
      }
      return true;

   }

   private String getMaxHeapSize(String pid, File jpsOutput) throws Exception {

      BufferedReader br = new BufferedReader(new FileReader(jpsOutput));;
      String maxHeap = "";

      String line = br.readLine();
      while(line != null){

         line = br.readLine();
         if( (line.substring(0, 10)).contains(pid)) {
            int start = line.indexOf("-Xmx");
            int end = line.indexOf(" ", start);
            maxHeap = line.substring(start, end);
            break;
         }
      }

      br.close();

      return maxHeap;

   }

}
