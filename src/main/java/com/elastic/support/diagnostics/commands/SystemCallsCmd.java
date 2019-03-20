package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.Constants;
import com.elastic.support.config.DiagConfig;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.*;

public class SystemCallsCmd implements Command {

    /**
     * Executes the system specific calls for this host that
     * the diagnostic is being run on. it will check the OS and pull the
     * proper command map from the config.
     */
    private static final Logger logger = LogManager.getLogger(SystemCallsCmd.class);

    public void execute(DiagnosticContext context) {

        // Check for Docker, which usually shows up as a PID of 1 for Elasticsearch
        String pid = context.getPid();
        if (pid.equals("1")) {
            logger.info("The node appears running in a Docker container since it shows a PID of 1.");
            logger.info("No system calls will be run. Utility should probably be run with --type remote.");
            return;
        }

        String os = checkOS();
        DiagConfig diagConfig = context.getDiagsConfig();
        Map<String, String> osCmds = diagConfig.getCommandMap(os);

        processCalls(context.getTempDir(), osCmds, pid);

    }

    public void processCalls(String targeDir, Map<String, String> osCmds, String pid) {

        ProcessBuilder pb = getProcessBuilder();

        try {
            osCmds = processArgsWithPid(osCmds, pid, SystemProperties.javaHome);

            Set<Map.Entry<String, String>> cmds = osCmds.entrySet();

            for(Map.Entry<String, String> ent: cmds) {
                String cmdLabel = ent.getKey();
                String cmdText = ent.getValue();
                runCommand(targeDir, cmdLabel, cmdText, pb);
            }

        } catch (Exception e) {
            logger.error("Error executing system calls.", e);
        }
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

    public Map<String, String> processArgsWithPid(Map<String, String> osCmds, String pid, String javaHome) {

        boolean pidPresent = true, javaHomeFound = true;

        // If we couldn't find a PID don't run the commands
        if (StringUtils.isEmpty(pid) || pid.equalsIgnoreCase(Constants.NOT_FOUND)) {
            logger.info("The diagnostic does not appear to be running on a host that contains a running node or that node could not be located in the retrieved list from the cluster.");
            logger.info("Some system calls will not be run. This utility should probably be run with --type remote.");
            pidPresent = false;
        }

        // If this is a JRE rather than a JDK there are commands we won't be able to run
        if (!isJdkPresent(SystemProperties.javaHome + SystemProperties.fileSeparator + "bin" + SystemProperties.fileSeparator + "javac")) {
            logger.info("Either JDK or Process Id was not present - bypassing those checks");
            javaHomeFound = false;
        }


        HashMap revMap = new HashMap();
        Iterator<Map.Entry<String, String>> iter = osCmds.entrySet().iterator();
        while (iter.hasNext()) {
            boolean addToRev = true;
            Map.Entry<String, String> entry = iter.next();
            String cmdKey = entry.getKey();
            String cmdText = entry.getValue();

            // Looks weird but cmd may have java home and a pid, and either might be missing.
            if (cmdText.contains("JAVA_HOME")) {
                if (javaHomeFound) {
                    cmdText = cmdText.replace("JAVA_HOME", javaHome);
                } else {
                    addToRev = false;
                }
            }

            if (cmdText.contains("PID")){
                if(pidPresent){
                    cmdText = cmdText.replace("PID", pid);
                }
                else{
                    addToRev = false;
                }
            }

            if(addToRev){
                revMap.put(cmdKey, cmdText);
            }
        }
        return revMap;
    }

    public void runCommand(String targetDir,
                           String cmdLabel,
                           String cmdText,
                           ProcessBuilder pb) {

        final List<String> cmds = new ArrayList<>();

        try {
            StringTokenizer st = new StringTokenizer(cmdText, " ");
            while (st.hasMoreTokens()) {
                cmds.add(st.nextToken());
            }
            logger.info("Running: " + cmdText);
            pb.redirectOutput(new File(targetDir + SystemProperties.fileSeparator + cmdLabel + ".txt"));
            pb.command(cmds);
            Process pr = pb.start();
            pr.waitFor();
        } catch (Exception e) {
            logger.error("Error processing system command:" + cmdLabel);
            try {
                FileOutputStream fos = new FileOutputStream(new File(targetDir + SystemProperties.fileSeparator + cmdLabel + ".txt"), true);
                PrintStream ps = new PrintStream(fos);
                e.printStackTrace(ps);
            } catch (Exception ie) {
                logger.error("Error processing system command", ie);
            }
        } finally {
            cmds.clear();
        }
    }

    public ProcessBuilder getProcessBuilder() {
        ProcessBuilder pb = new ProcessBuilder();
        pb.redirectErrorStream(true);
        return pb;
    }

    public boolean isJdkPresent(String pathToJavac) {
        try {
            File jdk = Paths.get(pathToJavac).toFile();
            if (jdk.exists()) {
                return true;
            }
        } catch (Exception e) {
            logger.debug("Error checking for JDK", e);
        }

        logger.info("JDK not found, assuming only JRE present.");
        return false;
    }


}

