/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.ProcessProfile;
import com.elastic.support.diagnostics.JavaPlatform;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.ResourceCache;
import com.elastic.support.util.SystemCommand;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public  class CollectLogs implements Command {

    private static final Logger logger = LogManager.getLogger(CollectLogs.class);

    public void execute(DiagnosticContext context) {
        // If we hit a snafu earlier in determining the details on where and how to run, then just get out.
        if(!context.runSystemCalls){
            logger.info(Constants.CONSOLE, "There was an issue in setting up system logs collection - bypassing. {}", Constants.CHECK_LOG);
            return;
        }

        // Should be cached from the PlatformDetails check.
        SystemCommand sysCmd = ResourceCache.getSystemCommand(Constants.systemCommands);
        String targetDir = context.tempDir + SystemProperties.fileSeparator + "logs";
        ProcessProfile targetNode = context.targetNode;
        JavaPlatform javaPlatform = targetNode.javaPlatform;
        String logDir = targetNode.logDir;
        String clusterName = context.clusterName;
        Map<String, Map<String, String>> osCmds = context.diagsConfig.getSysCalls(javaPlatform.platform);
        Map<String, String> logCalls = osCmds.get("logs");

        // Get the remote systemobject
        try{

            // Create the log directory - should never exist but always pays to be cautious.
            File tempLogDir = new File(targetDir);
            if(!tempLogDir.exists()){
                tempLogDir.mkdir();
            }

            // Build up a list of files to copy
            List<String> fileList = new ArrayList<>();

            // Check for the base elasticsearch log - will always be <clustername>.log
            // If it's not there, they probably have insufficient permissions so log it
            // to the console and exit gracefully.
            String logStatement = logCalls.get("elastic");
            logStatement = logStatement.replace("{{CLUSTERNAME}}", clusterName);
            logStatement = logStatement.replace("{{LOGPATH}}", logDir);
            String logListing = sysCmd.runCommand(logStatement).trim();
            if (StringUtils.isEmpty(logListing)) {
                logger.info(Constants.CONSOLE, "No Elasticsearch log could be located at the path configured for this node. The cause of this is usually insufficient read authority. Please be sure the account you are using to access the logs has the necessary permissions or use sudo. See the diagnostic README for more information. ");
                return;
            }

            fileList.add(logListing);
            logStatement = logCalls.get("elastic-arc");
            logStatement = logStatement.replace("{{CLUSTERNAME}}", clusterName);
            logStatement = logStatement.replace("{{LOGPATH}}", logDir);
            logListing = sysCmd.runCommand(logStatement).trim();
            fileList = extractFilesFromList(logListing, fileList, 2);

            logStatement = logCalls.get("gc");
            logStatement = logStatement.replace("{{LOGPATH}}", logDir);
            logListing = sysCmd.runCommand(logStatement).trim();
            fileList = extractFilesFromList(logListing, fileList, 3);

            sysCmd.copyLogs(fileList, logDir, targetDir );

        } catch (Exception e) {
            logger.info(Constants.CONSOLE, "An error occurred while copying the logs. It may not have completed normally {}.", Constants.CHECK_LOG);
            logger.error( e.getMessage(), e);
        }
    }

    protected List<String> extractFilesFromList(String output, List<String> fileList, int entries) {

        // Just in case, since NPE"s are a drag
        output = ObjectUtils.defaultIfNull(output, "");

        // If there's content add it to the file list
        if (StringUtils.isNotEmpty(output.trim())) {
            try {
                List<String> lines = IOUtils.readLines(new StringReader(output));
                int sz = lines.size();
                for (int i = 0; i < sz; i++) {
                    fileList.add(lines.get(i));
                    if(i == entries){
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error( "Error getting directory listing.", e);
            }
        }

        return fileList;

    }

}
