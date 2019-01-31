package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;

import java.util.Map;

public class SystemCallsCmd extends BaseSystemCallsCmd {

    public boolean execute(DiagnosticContext context) {

        String pid = context.getPid();
        if (pid.equals("1")) {
            logger.info("The node appears running in a Docker container since it shows a PID of 1.");
            logger.info("No system calls will be run. Utility should probably be run with --type remote.");
            return true;
        }

        if (pid.equalsIgnoreCase("not found")) {
            logger.info("The diagnostic does not appear to be running on a host that contains a running node or that node could not be located in the retrieved list from the cluster.");
            logger.info("No system calls will be run. Utility should probably be run with --type remote.");
            return true;
        }

        try {
            String os = checkOS();
            Map<String, String> osCmds = (Map<String, String>) context.getConfig().get(os);
            String javaHome = SystemProperties.javaHome;
            if (!isJdkPresent()) {
                logger.info("Either JDK or Process Id was not present - bypassing those checks");
                osCmds.remove("jps");
                osCmds.remove("jstack");
                return true;
            }
            osCmds = processArgs(osCmds, pid, javaHome);
            processCalls(context, osCmds);
        } catch (Exception e) {
            logger.error("Error executing one or more system calls.", e);
        }

        return true;

    }

}

