package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagnosticException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CollectLocalSystemCalls extends BaseSystemCallCollection  implements Command {

    private static final Logger logger = LogManager.getLogger(CollectLocalSystemCalls.class);

    public void execute(DiagnosticContext context) {

        if (context.isBypassSystemCalls()) {
            logger.log(SystemProperties.DIAG, "Could not locate local node - bypassing system stats calls.");
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
                JavaPlatform javaPlatform = checkOS( SystemProperties.osName.toLowerCase());

                // Get the configurations for that platoform's sys calls.
                Map<String, Map<String, String>> osCmds = context.getDiagsConfig().getSysCalls(javaPlatform.platform);
                Map<String, String> sysCalls = osCmds.get("sys");

                processCalls(tempDir, sysCalls, pb);

                Map<String, String> esProcess = osCmds.get("es");
                processCalls(tempDir, esProcess, pb);

                // Execute platform specific search for Java/Elastic process(es)
                File output = FileUtils.getFile(tempDir, "elastic-process.txt");
                String  esJDKCmdOutput = FileUtils.readFileToString(output, javaPlatform.cmdEndcoding);

                // For the JDK based commoands we need to execute them using the same
                // JVM that's running ES. Given that it could be the bundled one or some
                // previously installed version we need to shell out and check before we run them.
                String esJavaHome = parseEsProcessString(esJDKCmdOutput, javaPlatform);
                logger.info("Using Java installation at: {}", esJavaHome);
                if (isJdkPresent(esJavaHome, javaPlatform)) {
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
                logger.info("Unexpected error - bypassing some or all system calls. {}", Constants.CHECK_LOG);
            }
        }
    }

    public void processCalls(String targetDir, Map<String, String> commandMap, ProcessBuilder pb) {

        commandMap.forEach((k, v) -> {
                    runCommand(targetDir, k, v, pb);
                }
        );
    }

    public JavaPlatform checkOS(String osName) {
        if (osName.contains("windows")) {
            return new JavaPlatform(Constants.winPlatform);
        } else if (osName.contains("linux")) {
            return new JavaPlatform(Constants.linuxPlatform);
        } else if (osName.contains("darwin") || osName.contains("mac os x")) {
            return new JavaPlatform(Constants.macPlatform);
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

    public String parseEsProcessString(String input, JavaPlatform javaPlatform) {

        String line;
        try (BufferedReader br = new BufferedReader(new StringReader(input))) {
            while ((line = br.readLine()) != null) {
                if (line.contains(javaPlatform.javaExecutable) && line.contains("-Des.")) {
                    String javaHome =  extractJavaHome(line, javaPlatform);
                    return javaPlatform.extraProcess(javaHome);
                }
                else {
                    continue;
                }
            }
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Error obtaining the path for the JDK", e);
        }

        // If we got this far we couldn't find a JDK
        logger.info("Could not locate the location of the java executable used to launch Elasticsearch");
        throw new DiagnosticException("JDK not found.");
    }

    String extractJavaHome(String processString, JavaPlatform javaPlatform){
        String wsRegex = "\\s";
        for(int i=0; i < javaPlatform.whitespaceCount; i++){
            // There will be a variable number of whitespace occurences in from of the java path
            // dependent on platform. Strip off the preceding column and then the whitespace that was found
            // after it until you get to the magic index.
            int wsIndex =  regexIndexOf(processString, wsRegex);
            processString = processString.substring(wsIndex);
            processString = processString.trim();

        }

        // After the preceding cols are stripped, truncate the output behind the path to the executable.
        int jpathIndex = processString.indexOf(javaPlatform.javaExecutable);
        processString = processString.substring(0, jpathIndex);

        return processString;
    }

    private int regexIndexOf(String input, String regex){
        Pattern pat = Pattern.compile(regex);
        Matcher m = pat.matcher(input);
        if ( m.find() ) {
            return m.start();
        }
        return -1;
    }

    public boolean isJdkPresent(String javaHome, JavaPlatform javaPlatform) {

        try {
            String javacPath = javaHome + SystemProperties.fileSeparator + javaPlatform.javaCompiler;
            File jdk = Paths.get(javacPath).toFile();
            if (jdk.exists()) {
                return true;
            }
        } catch (Exception e) {
            logger.debug("Error checking for JDK", e);
        }

        logger.info("JDK not found - bypassing jstack and jps commands.");
        return false;
    }

    class JavaPlatform {

        public String platform;
        String javaExecutable ="/bin/java";
        String javaCompiler = "/bin/javac";
        String cmdEndcoding = "UTF-8";
        int whitespaceCount = 7;

        JavaPlatform(String platform){
            this.platform = platform;
            if(platform.equalsIgnoreCase(Constants.winPlatform)){
                javaExecutable = "\\bin\\java.exe";
                javaCompiler = "\\bin\\javac.exe";
                // Output from wmic in Win outputs as a different character set.
                cmdEndcoding = "UTF-16";
                whitespaceCount = 1;
            }
            else if(platform.equals(Constants.macPlatform)){
                whitespaceCount = 4;
            }
        }

        String extraProcess(String input){
            // Get rid of the whitespace in front
            if (platform.equals(Constants.winPlatform)){
                input = input.replace("\"", "");
            }

            return input;
        }
    }
}

