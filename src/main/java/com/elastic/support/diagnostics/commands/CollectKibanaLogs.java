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

/**
 * This class is executed as CollectKibanaLogs class.
 * The logs in Kibana are not placed always in the same place, if you use RPM to install it logs will be accesible with journalctl
 * But if you use the debian package the logs will be in /var/log/kibana/, and if you install it manually or in Windows the default will be stdout.
 * The Kibana API do not expose where the logs are stored, so we will try in /var/log/kibana/ if not we will check journalctl, and if any of those work we will not collect any logs file.
*/

public  class CollectKibanaLogs implements Command {

    private static final Logger logger = LogManager.getLogger(CollectLogs.class);

    /**
    * We will get the information fron the DiagnosticContext to extract and copy the logs from Local or Remote system.
    *
    * @param  DiagnosticContext context
    * @return void
    */
    public void execute(DiagnosticContext context) {
        // If we hit a snafu earlier in determining the details on where and how to run, then just get out.
        if(!context.runSystemCalls){
            logger.info(Constants.CONSOLE, "There was an issue in setting up system logs collection - bypassing. {}", Constants.CHECK_LOG);
            return;
        }
        // the defaults are only set for RPM / Debian or Homebrew 
        if (context.targetNode.os.equals(Constants.winPlatform)) {
            logger.info(Constants.CONSOLE, "Kibana logs can not be collected for Windows, the log path is not shared in the Kibana APIs and there is no defaults.");
            return;
        }

        SystemCommand sysCmd = ResourceCache.getSystemCommand(Constants.systemCommands);
        String targetDir = context.tempDir + SystemProperties.fileSeparator + "logs";
        ProcessProfile targetNode = context.targetNode;
        JavaPlatform javaPlatform = targetNode.javaPlatform;
        Map<String, Map<String, String>> osCmds = context.diagsConfig.getSysCalls(javaPlatform.platform);
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
            // Build up a list of files to copy
            List<String> fileList = new ArrayList<>();
            fileList = extractFilesFromList(logListing, fileList, 3);
            String kibanaPath = logCalls.get("kibana-default-path");
            sysCmd.copyLogs(fileList, kibanaPath, targetDir);
            logger.info(Constants.CONSOLE, "Collecting logs from Kibana default path.");
        } catch (Exception e) {
            logger.info(Constants.CONSOLE, "An error occurred while copying the logs. It may not have completed normally {}.", Constants.CHECK_LOG);
            logger.error( e.getMessage(), e);
        }
    }

     /**
    * this private function will create a new JavaPlatform object
    *
    * @param  String output , is the text returned by the ls/dir command
    * @param  List<String> fileList , You can call this function multitple times, and add more files to the List
    * @param  int entries , How many time we will iterate
    * @return List<String>
    */
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
