package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagConfig;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CollectDockerInfo implements Command {

    private static final Logger logger = LogManager.getLogger(CollectDockerInfo.class);

  public void execute(DiagnosticContext context) {

      SystemCommand systemCommand = ResourceCache.getSystemCommand(Constants.systemCommands);
      String targetDir = context.tempDir + SystemProperties.fileSeparator + "docker";

      // Run the global calls. It's a single pass
      runDockerCalls(targetDir, context.diagsConfig.dockerGlobal, systemCommand, "", context.diagsConfig.dockerExecutablePath);

      String idsCmd = context.diagsConfig.dockerContainerIds;
      if(systemCommand instanceof RemoteSystem){
          idsCmd = context.diagsConfig.dockerExecutablePath + idsCmd;
      }
      List<String> containerIds = getDockerContainerIds(systemCommand, idsCmd);
      for(String container: containerIds){
          runDockerCalls(targetDir, context.diagsConfig.dockerContainer, systemCommand, container, context.diagsConfig.dockerExecutablePath);
      }

  }

    public List<String> getDockerContainerIds(SystemCommand systemCommand, String idsCmd) {

        try {
            String output = systemCommand.runCommand(idsCmd);

            // If there's content add it to the file list
            if (StringUtils.isNotEmpty(output.trim())) {
                try {
                    List<String> lines = IOUtils.readLines(new StringReader(output));
                    return lines;

                } catch (Exception e) {
                    logger.log(SystemProperties.DIAG, "Error getting directory listing.", e);
                }
            }

        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Error obtaining Docker Container Id's");
        }

        // If anything happened just bypass processing and continue with the rest.
        return new ArrayList<String>();

    }

    private void runDockerCalls(String targetDir, Map<String, String> commandMap, SystemCommand sysCmd, String token, String dockerExecutablePath) {
        String suffix = "";
        if (StringUtils.isNotEmpty(token)) {
            suffix = "-" + token;
        }

        for (Map.Entry<String, String> entry : commandMap.entrySet()) {
            try {
                String cmd = entry.getValue().replace("{{CONTAINER_ID}}", token);
                if(sysCmd instanceof RemoteSystem){
                    cmd = dockerExecutablePath + cmd;
                }
                String output = sysCmd.runCommand(cmd);
                SystemUtils.writeToFile(output, targetDir + SystemProperties.fileSeparator + entry.getKey() + suffix + ".txt");
            } catch (Exception e) {
                logger.info(e.getMessage());
            }
        }

    }

}
