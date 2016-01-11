package com.elastic.support.diagnostics.commands;

import com.elastic.support.SystemProperties;
import com.elastic.support.diagnostics.DiagnosticContext;

import java.io.File;
import java.util.*;

public class SystemCallsCmd extends AbstractDiagnosticCmd {

    public boolean execute(DiagnosticContext context){

            String os = checkOS();
            Map<String, String> osCmds = (Map<String, String>) context.getConfig().get(os);

            ProcessBuilder pb = new ProcessBuilder();
            pb.redirectErrorStream(true);

            Iterator<Map.Entry<String, String>> iter = osCmds.entrySet().iterator();
            List cmds = new ArrayList();

            try {
                while (iter.hasNext()) {
                    Map.Entry<String, String> entry = (Map.Entry<String, String>) iter.next();
                    String cmdLabel = entry.getKey();
                    String cmdText = entry.getValue();

                    // One off hack for process limits
                    if (cmdLabel.equals("proc-limit")){
                        //cmdText.replaceAll("PID",  );
                        cmdText = "cat /proc/" + context.getPid() + "/limits";
                    }

                    StringTokenizer st = new StringTokenizer(cmdText, " ");
                    while (st.hasMoreTokens()) {
                        cmds.add(st.nextToken());
                    }

                    pb.redirectOutput(new File(context.getTempDir() + SystemProperties.fileSeparator + cmdLabel + ".txt"));
                    pb.command(cmds);
                    Process pr = pb.start();
                    pr.waitFor();
                    cmds.clear();

                }
            } catch (Exception e) {
                logger.error("Error processing system commands", e);
            }

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
