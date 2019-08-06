package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.Constants;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
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

        if(context.isBypassSystemCalls()){
            logger.log(SystemProperties.DIAG, "Identified Docker installations or could not locate local node - bypassing system stats calls.");
            return;
        }

        String tempDir = context.getTempDir();
        String pid = context.getPid();
        ProcessBuilder pb = getProcessBuilder();

        if (Constants.NOT_FOUND.equals(pid)) {
            logger.info("The host this utility is running on could not be found in the cluster - Bypassing system calls");
            return;
        } else if (context.isDocker()) {
            // If docker is running and we got an id passed in check to see if it's
            // in the list of container id's we obtained in the host check. If it is
            // just get that one. If it's not there or there was no id go get all the containers in the list.
            List<String> containers = context.getDockerContainers();
            String dockerId = context.getDiagnosticInputs().getDockerId();
            if (StringUtils.isNotEmpty(dockerId)) {
                boolean isDockerIdInList = containers.contains(dockerId);
                if (isDockerIdInList) {
                    containers.clear();
                    containers.add(dockerId);
                }
            }

            // Run the non-container specific docker calls
            Map<String, String> dockerGlobal = context.getDiagsConfig().getCommandMap("docker-global");
            processCalls(tempDir, dockerGlobal, pb);

            Map<String, String> dockerContainers = context.getDiagsConfig().getCommandMap("docker-containers");
            // Now get the stats for each container in the list. If they passed one in and we found it this will
            // be a single iteration
            for (String container : containers) {
                try {
                    String dir = context.getTempDir() + SystemProperties.fileSeparator + container;
                    Files.createDirectories(Paths.get(dir));
                    Map<String, String> containerCalls = preProcessDockerCommands(dockerContainers, container);
                    processCalls(dir, containerCalls, pb);
                } catch (IOException e) {
                    logger.log(SystemProperties.DIAG, e);
                    logger.info("Error creating container directory - bypassing Docker container calls.");
                }
                catch (Exception e){
                    logger.log(SystemProperties.DIAG, e);
                    logger.info("Unexpected error - bypassing Docker container calls.");
                }
            }
        } else {
            try{
                String calls = checkOS();
                Map<String, String> osCmds = context.getDiagsConfig().getCommandMap(calls);
                osCmds = preProcessOsCmds(osCmds, pid, SystemProperties.javaHome);
                processCalls(tempDir, osCmds, pb);
            }
            catch (Exception e){
                logger.log(SystemProperties.DIAG, e);
                logger.info("Unexpected error - bypassing System calls.");
            }

        }

    }

    public void processCalls(String targetDir, Map<String, String> commandMap, ProcessBuilder pb) {

        commandMap.forEach((k, v) -> {
                    runCommand(targetDir, k, v, pb);
                }
        );
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
            throw new RuntimeException("Unsupported OS - " + osName);
        }
    }

    public Map<String, String> preProcessOsCmds(Map<String, String> osCmds, String pid, String javaHome) {

        // If this is a JRE rather than a JDK there are commands we won't be able to run
        if (!isJdkPresent(SystemProperties.javaHome + SystemProperties.fileSeparator + "bin" + SystemProperties.fileSeparator + "javac")) {
            logger.info("JDK was not present - bypassing jps and jstack");
            osCmds.remove("jps");
            osCmds.remove("jstack");
        }

        Map<String, String> revMap = new HashMap();
        osCmds.forEach((k, v) -> {
            // Looks weird but cmd may have java home and a pid, and either might be missing.
            String cmdText = v;
            if (cmdText.contains("JAVA_HOME")) {
                cmdText = cmdText.replace("JAVA_HOME", javaHome);
            }

            if (cmdText.contains("PID")) {
                cmdText = cmdText.replace("PID", pid);
            }

            revMap.put(k, cmdText);

        });

        return revMap;
    }

    public Map<String, String> preProcessDockerCommands(Map<String, String> cmds, String container){

        Map<String, String> revMap = new HashMap<>();
        cmds.forEach((k, v) ->{
            String cmdText = v.replace("CONTAINER_ID", container);
            revMap.put(k, cmdText);
        });

        return revMap;

    }

    public String runCommand(String command){
        Runtime rt = Runtime.getRuntime();
        command.replace("'", "");
        try {
            Process ps = rt.exec(command);
            ps.waitFor();
            InputStreamReader processReader = new InputStreamReader(ps.getErrorStream());
            BufferedReader br = new BufferedReader(processReader);
            String line, result = "";
            while((line = br.readLine()) != null){
                logger.info(line);
            }

            ps.destroy();
            logger.info(result);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";

    }

    public void runCommand(String targetDir,
                           String cmdLabel,
                           String cmdText,
                           ProcessBuilder pb) {

        List<String> cmds = buildCommandTokens(cmdText);

        try {
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

    public List<String> buildCommandTokens(String cmdText) {
        List<String> cmds = new ArrayList<>();

        // Check for single quote arguments in Docker commands
        // 2 entry array if it has them.
        String[] args = cmdText.split(" ;; ");

        // Tokenize the first section.
        StringTokenizer st = new StringTokenizer(args[0], " ");
        while (st.hasMoreTokens()) {
            cmds.add(st.nextToken());
        }

        // If there was a quote delimited set of parameters to
        // pass it in add it now, sans quote.
        if(args.length > 1){
            cmds.add(args[1]);
        }

        return cmds;

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

