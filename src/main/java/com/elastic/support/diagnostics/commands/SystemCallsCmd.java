package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.DiagConfig;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class SystemCallsCmd extends BaseSystemCallsCmd {

    /** Executes the system specific calls for this host that
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

        // If we couldn't find a PID don't run the commands
        if (pid.equalsIgnoreCase("not found")) {
            logger.info("The diagnostic does not appear to be running on a host that contains a running node or that node could not be located in the retrieved list from the cluster.");
            logger.info("No system calls will be run. Utility should probably be run with --type remote.");
            return;
        }

        try {
            String os = checkOS();
            DiagConfig diagConfig = context.getDiagsConfig();
            Map<String, String> osCmds = diagConfig.getCommandMap(os);
            String javaHome = SystemProperties.javaHome;
            // If this is a JRE rather than a JDK there are commands we won't be able to run
            if (!isJdkPresent()) {
                logger.info("Either JDK or Process Id was not present - bypassing those checks");
                osCmds.remove("jps");
                osCmds.remove("jstack");
                return;
            }
            osCmds = processArgs(osCmds, pid, javaHome);
            processCalls(context, osCmds);
        } catch (Exception e) {
            logger.error("Error executing one or more system calls.", e);
        }


    }

}

