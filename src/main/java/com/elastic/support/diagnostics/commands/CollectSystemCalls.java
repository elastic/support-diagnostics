package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.ProcessProfile;
import com.elastic.support.diagnostics.JavaPlatform;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.ResourceCache;
import com.elastic.support.util.SystemCommand;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import com.sun.xml.bind.v2.runtime.reflect.opt.Const;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Map;

public class CollectSystemCalls implements Command {

    private static final Logger logger = LogManager.getLogger(CollectSystemCalls.class);

    public void execute(DiagnosticContext context) {

        // If we hit a snafu earlier in determining the details on where and how to run, then just get out.
        if(!context.runSystemCalls){
            logger.info( Constants.CONSOLE, "There was an issue in setting up system call collection - bypassing. {}", Constants.CHECK_LOG);
            return;
        }

        // Should be cached from the PlatformDetails check.
        SystemCommand sysCmd = ResourceCache.getSystemCommand(Constants.systemCommands);
        String targetDir = context.tempDir + SystemProperties.fileSeparator + "syscalls";
        String pid = context.targetNode.pid;
        ProcessProfile targetNode = context.targetNode;
        JavaPlatform javaPlatform = targetNode.javaPlatform;
        Map<String, Map<String, String>> osCmds = context.diagsConfig.getSysCalls(javaPlatform.platform);

        try {
            // Get the configurations for that platoform's sys calls.
            Map<String, String> sysCalls = osCmds.get("sys");
            processCalls(targetDir, sysCalls, sysCmd, pid);
            logger.info( Constants.CONSOLE, "First set of system calls executed.");
            // This should give us the full path to the java executable that
            // was used to start Elasticsearch
            Map<String, String> javaCalls = osCmds.get("java");
            String javaProcessString = javaCalls.get("elastic-java");
            javaProcessString = javaProcessString.replace("{{PID}}", pid);
            String javaExecutablePath = sysCmd.runCommand(javaProcessString);

            // For the JDK based commoands we need to execute them using the same
            // JVM that's running ES. Given that it could be the bundled one or some
            // previously installed version we need to shell out and check before we run them.
            String esJavaHome = javaPlatform.extractJavaHome(javaExecutablePath);
            logger.info(Constants.CONSOLE, "Java Home installation at: {}", esJavaHome);

            // Check for the presence of a JDK - run the javac command with no arguments
            // and see if you get a valid return - javac <version>

            String javacCheck = javaCalls.get("javac");
            javacCheck = javacCheck.replace("{{JAVA_HOME}}", esJavaHome);
            javaCalls.put("javac", javacCheck);
            String javacResult = sysCmd.runCommand(javacCheck);
            if(javacResult.toLowerCase().contains(javaPlatform.javac)){

                String jstack = javaCalls.get("jstack");
                jstack = jstack.replace("{{JAVA_HOME}}", esJavaHome);
                jstack = jstack.replace("{{PID}}", pid);

                String jps = javaCalls.get("jps");
                jps = jps.replace("{{JAVA_HOME}}", esJavaHome);

                javaCalls.put("jstack", jstack);
                javaCalls.put("jps", jps);
                processCalls(targetDir, javaCalls, sysCmd, pid);
            }
            else {
                logger.info( Constants.CONSOLE, "JDK not found - bypassing jstack and jps commands.");
            }
        } catch (Exception e) {
            logger.error( e);
            logger.info(Constants.CONSOLE, "Unexpected error - bypassing some or all system calls. {}", Constants.CHECK_LOG);
        }
    }

    public static void processCalls(String targetDir, Map<String, String> commandMap, SystemCommand sysCmd, String pid) {
        commandMap.forEach((k, v) -> {
                    try {
                        String cmd = v.replace("{{PID}}", pid);
                        String output = sysCmd.runCommand(cmd);
                        SystemUtils.writeToFile(output, targetDir + SystemProperties.fileSeparator + k + ".txt");
                    } catch (Exception e) {
                        logger.info(e.getMessage());
                    }
                }
        );
    }

}
