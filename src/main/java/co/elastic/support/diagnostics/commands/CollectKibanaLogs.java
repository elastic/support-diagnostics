/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.Constants;
import co.elastic.support.diagnostics.ProcessProfile;
import co.elastic.support.diagnostics.chain.Command;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.util.ResourceCache;
import co.elastic.support.util.SystemCommand;
import co.elastic.support.util.SystemProperties;
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

/**
 * Kibana do not provide any API that expose the logs path
 * This class will check if the logs are in the default path and if they are collect them.
 * 
 */
public  class CollectKibanaLogs implements Command {

    private static final Logger logger = LogManager.getLogger(CollectLogs.class);

   /**
    * We will get the information fron the DiagnosticContext to extract and copy the logs from Local or Remote system.
    *
    * @param  DiagnosticContext context
    */
    public void execute(DiagnosticContext context) {
        // If we hit a snafu earlier in determining the details on where and how to run, then just get out.
        if(!context.runSystemCalls){
            logger.info(Constants.CONSOLE, "There was an issue in setting up system logs collection - bypassing. {}", Constants.CHECK_LOG);
            return;
        }
        // the defaults are only set for RPM / Debian or Homebrew, in windows we can not collect any Kibana logs.
        if (context.targetNode.os.equals(Constants.winPlatform)) {
            logger.info(Constants.CONSOLE, "Kibana logs can not be collected for Windows, the log path is not shared in the Kibana APIs and there is no defaults.");
            return;
        }

        SystemCommand sysCmd = ResourceCache.getSystemCommand(Constants.systemCommands);
        String targetDir = context.tempDir + SystemProperties.fileSeparator + "logs";
        ProcessProfile targetNode = context.targetNode;

        Map<String, Map<String, String>> osCmds = context.diagsConfig.getSysCalls(context.targetNode.os);
        Map<String, String> logCalls = osCmds.get("logs");

        try{
            // Create the log directory - should never exist but always pays to be cautious.
            File tempLogDir = new File(targetDir);
            if(!tempLogDir.exists()){
                tempLogDir.mkdir();
            }
            String logStatement = logCalls.get("kibana");
            String logListing = sysCmd.runCommand(logStatement).trim();
            if (StringUtils.isEmpty(logListing) || logListing.contains("No such file or directory")) {
                sysCmd.copyLogsFromJournalctl("kibana.service", targetDir);
                logger.info(Constants.CONSOLE, "No Kibana logs could be located at the default path. Searching for Journalctl logs with kibana.service name.");
                return;
            }
            List<String> fileList = extractFilesFromList(logListing, 3);
            String kibanaPath = logCalls.get("kibana-default-path");
            sysCmd.copyLogs(fileList, kibanaPath, targetDir);
            logger.info(Constants.CONSOLE, "Collecting logs from Kibana default path.");
        } catch (Exception e) {
            logger.info(Constants.CONSOLE, "An error occurred while copying the logs. It may not have completed normally {}.", Constants.CHECK_LOG);
            logger.error( e.getMessage(), e);
        }
    }

   /**
    * According with the OS response, read and extract the content for the logs files.
    *
    * @param output is the text returned by the <code>ls</code>/<code>dir</code> command
    * @param fileList You can call this function multitple times, and add more files to the List
    * @param entries How many time we will iterate
    * @return Never {@code null}. Always the number specified by {@code entries}.
    */
    protected List<String> extractFilesFromList(String output, int entries) {
        List<String> fileList = new ArrayList<>(entries);

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
