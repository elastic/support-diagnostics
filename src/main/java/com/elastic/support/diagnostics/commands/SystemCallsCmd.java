package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.Constants;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SystemCallsCmd implements Command {

    private static final Logger logger = LogManager.getLogger(SystemCallsCmd.class);
    private String esProcessString;

    public void execute(DiagnosticContext context) {

        if (context.isBypassSystemCalls()) {
            logger.log(SystemProperties.DIAG, "Identified Docker installations or could not locate local node - bypassing system stats calls.");
            return;
        }

        String tempDir = context.getTempDir();
        String pid = context.getPid();
        ProcessBuilder pb = getProcessBuilder();

        if (context.isDocker()) {
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
                } catch (Exception e) {
                    logger.log(SystemProperties.DIAG, e);
                    logger.info("Unexpected error - bypassing Docker container calls.");
                }
            }
        } else {
            try {
                // Will tell us if we're on Linux, Win or OSX
                String platform = checkOS();

                // Get the configurations for that platoform's sys calls.
                Map<String, Map<String, String>> osCmds = context.getDiagsConfig().getSysCalls(platform);
                Map<String, String> sysCalls = osCmds.get("sys");

                processCalls(tempDir, sysCalls, pb);

                // Figure out where the Java runtime exists and whether it's a JDK or not?
                //osCmds = preProcessOsCmds(sysCalls, pid, SystemProperties.javaHome);
                Map<String, String> esProcess = osCmds.get("es");
                processCalls(tempDir, esProcess, pb);

                File output = FileUtils.getFile(tempDir, "elastic-process.txt");
                String  esJDKCmdOutput = null;
                // Output from wmic in Win outputs as a different character set.
                if(platform.equals("winOS")){
                    esJDKCmdOutput = FileUtils.readFileToString(output, "UTF-16");
                }
                else{
                    esJDKCmdOutput = FileUtils.readFileToString(output, "UTF-8");
                }

                // For the JDK based commoands we need to execute them using the same
                // JVM that's running ES. Given that it could be the bundled one or some
                // previously installed version we need to shell out and check before we run them.
                String esJavaHome = parseEsProcessString(esJDKCmdOutput, SystemProperties.fileSeparator, platform);
                logger.info("Using Java installation at: {}", esJavaHome);
                if (isJdkPresent(esJavaHome, platform)) {
                    Map<String, String> javaCalls = osCmds.get("java");

                    String jstack = javaCalls.get("jstack");
                    jstack = jstack.replace("JAVA_HOME", esJavaHome);
                    jstack = jstack.replace("PID", pid);

                    String jps = javaCalls.get("jps");
                    jps = jps.replace("JAVA_HOME", esJavaHome);

                    javaCalls.put("jstack", jstack);
                    javaCalls.put("jps", jps);
                    processCalls(tempDir, javaCalls, pb);
                }
            } catch (Exception e) {
                logger.log(SystemProperties.DIAG, e);
                logger.info("Unexpected error - bypassing System calls. {}", Constants.CHECK_LOG);
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

    public Map<String, String> preProcessDockerCommands(Map<String, String> cmds, String container) {

        Map<String, String> revMap = new HashMap<>();
        cmds.forEach((k, v) -> {
            String cmdText = v.replace("CONTAINER_ID", container);
            revMap.put(k, cmdText);
        });
        return revMap;
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
        if (args.length > 1) {
            cmds.add(args[1]);
        }
        return cmds;
    }

    public String parseEsProcessString(String input, String fileSeparator, String platform) {

        String javaExePath = fileSeparator + "bin" + fileSeparator + "java";
        // Check each line to see if it's an ES java process...
        if (StringUtils.isEmpty(input)) {
            return "";
        }
        // Windows output is enclosed in quotes
        if(platform.equals("winOS")){
            input = input.replace("\"", "");
        }

        String line;
        try (BufferedReader br = new BufferedReader(new StringReader(input))) {
            String path;
            while ((line = br.readLine()) != null) {
                if (!line.contains(javaExePath)) {
                    continue;
                }
                path = "";
                String[] lineArray = line.split("\\s+");

                for (String token : lineArray) {
                    if (token.contains(javaExePath)) {
                        path = token;
                    }
                    // If there are ES parameters on the tokenized command line we can probaby assume this is it.
                    if (StringUtils.isNotEmpty(path) && token.contains("-Des.")) {
                        int idx = path.lastIndexOf(fileSeparator + "bin");
                        return path.substring(0, idx);
                    }
                }
            }
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Error obtaining the path for the JDK", e);
        }
        return "";
    }

    public boolean isJdkPresent(String javaHome, String platform) {
        if (StringUtils.isNotEmpty(javaHome)) {
            try {
                String javac = "javac";
                if(platform.equals("winOS")){
                    javac = "javac.exe";
                }

                String javacPath = javaHome + SystemProperties.fileSeparator + "bin" + SystemProperties.fileSeparator + javac;
                File jdk = Paths.get(javacPath).toFile();
                if (jdk.exists()) {
                    return true;
                }
            } catch (Exception e) {
                logger.debug("Error checking for JDK", e);
            }
        }
        logger.info("JDK not found - bypassing jstack and jps commands.");
        return false;
    }
}

