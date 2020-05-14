package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CollectDockerInfo implements Command {

    private static final Logger logger = LogManager.getLogger(CollectDockerInfo.class);

    public void execute(DiagnosticContext context) {

        SystemCommand systemCommand = ResourceCache.getSystemCommand(Constants.systemCommands);

        // Run the system calls first to get the host's stats
        String targetDir = context.tempDir + SystemProperties.fileSeparator + "syscalls";
        String pid = "1";
        String platform = Constants.linuxPlatform;
        if (systemCommand instanceof LocalSystem) {
            SystemUtils.parseOperatingSystemName(SystemProperties.osName);
        }

        Map<String, Map<String, String>> osCmds = context.diagsConfig.getSysCalls(platform);
        Map<String, String> sysCalls = osCmds.get("sys");
        CollectSystemCalls.processCalls(targetDir, sysCalls, systemCommand, pid);

        targetDir = context.tempDir + SystemProperties.fileSeparator + "docker";

        // Determine where Docker is located
        String dockerPath = getDockerPath(systemCommand, platform);
        //String kubePath = getKubectlPath(dockerPath);

        // Run the global calls. It's a single pass
        runDockerCalls(targetDir, context.diagsConfig.dockerGlobal, systemCommand, "", dockerPath);
        //runDockerCalls(targetDir, context.diagsConfig.kubernates, systemCommand, "", kubePath);

        String idsCmd = context.diagsConfig.dockerContainerIds;

        List<String> containerIds = getDockerContainerIds(systemCommand, idsCmd, dockerPath);
        for (String container : containerIds) {
            runDockerCalls(targetDir, context.diagsConfig.dockerContainer, systemCommand, container, dockerPath);
        }
    }

    public List<String> getDockerContainerIds(SystemCommand systemCommand, String idsCmd, String dockerPath) {

        try {
            idsCmd = idsCmd.replace("{{dockerPath}}", dockerPath);
            String output = systemCommand.runCommand(idsCmd);

            // If there's content add it to the file list
            if (StringUtils.isNotEmpty(output.trim())) {
                try {
                    List<String> lines = IOUtils.readLines(new StringReader(output));
                    return lines;

                } catch (Exception e) {
                    logger.error( "Error getting directory listing.", e);
                }
            }

        } catch (Exception e) {
            logger.error( "Error obtaining Docker Container Id's");
        }

        // If anything happened just bypass processing and continue with the rest.
        return new ArrayList<String>();

    }

    private void runDockerCalls(String targetDir, Map<String, String> commandMap, SystemCommand sysCmd, String token, String dockerPath) {
        String suffix = "";
        if (StringUtils.isNotEmpty(token)) {
            suffix = "-" + token;
        }

        for (Map.Entry<String, String> entry : commandMap.entrySet()) {
            try {
                String cmd = entry.getValue().replace("{{CONTAINER_ID}}", token);
                cmd = cmd.replace("{{dockerPath}}", dockerPath);

                String output = sysCmd.runCommand(cmd);
                SystemUtils.writeToFile(output, targetDir + SystemProperties.fileSeparator + entry.getKey() + suffix + ".txt");
            } catch (Exception e) {
                logger.error(Constants.CONSOLE, e.getMessage());
            }
        }

    }

    private String getDockerPath(SystemCommand systemCommand, String platform) {

        if (platform.equalsIgnoreCase(Constants.winPlatform)) {
            return "docker";
        }

        for(String path: Constants.exePaths){
            String expectedPath = path + "docker";
            String dockerPath = systemCommand.runCommand("ls " + expectedPath);
            dockerPath = dockerPath.trim();
            if(expectedPath.equalsIgnoreCase(dockerPath)){
                return dockerPath;
            }
        }

        return "docker";
    }

    private String getKubectlPath(String dockerPath){

        return dockerPath.replace("docker", "kubectl");

    }

}
