package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class LogCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      if (context.getInputParams().isSkipLogs() || !context.isLocalAddressLocated()) {
         return true;
      }

      JsonNode diagNode = context.getTypedAttribute("diagNode", JsonNode.class);
      if (diagNode == null) {
         logger.error("Could not locate node running on current host.");
         return true;
      }

      boolean getAccess = context.getInputParams().isAccessLogs();
      logger.info("Processing log files.");

      JsonNode settings = diagNode.path("settings");

      String name = diagNode.path("name").asText();
      context.setAttribute("diagNodeName", name);

      String clusterName = context.getClusterName();
      JsonNode nodePaths = settings.path("path");
      JsonNode defaultPaths = settings.path("default").path("path");

      String logs = nodePaths.path("logs").asText();
      String home = nodePaths.path("home").asText();
      String defaultLogs = defaultPaths.path("logs").asText();

      try {
         int maxLogs =  SystemUtils.toInt(context.getConfig().get("maxLogs"), 3 );
         int maxGcLogs =  SystemUtils.toInt(context.getConfig().get("maxGcLogs"), 3 );

         List<String> fileDirs = new ArrayList<>();
         context.setAttribute("tempFileDirs", fileDirs);

         // Create a directory for this node
         String nodeDir = context.getTempDir() + SystemProperties.fileSeparator + "logs";

         fileDirs.add(nodeDir);

         Files.createDirectories(Paths.get(nodeDir));
         File logDest = new File(nodeDir);
         logs = determineLogLocation(home, logs, defaultLogs);
         File logDir = new File(logs);
         if (logDir.exists()) {
            if (context.getInputParams().isArchivedLogs()) {
               FileUtils.copyDirectory(logDir, logDest, true);
            } else {
               //Get the top level log, slow search, and slow index logs
               FileUtils.copyFileToDirectory(new File(logs + SystemProperties.fileSeparator + clusterName + ".log"), logDest);
               FileUtils.copyFileToDirectory(new File(logs + SystemProperties.fileSeparator + clusterName + "_index_indexing_slowlog.log"), logDest);
               FileUtils.copyFileToDirectory(new File(logs + SystemProperties.fileSeparator + clusterName + "_index_search_slowlog.log"), logDest);
               if (getAccess) {
                  FileUtils.copyFileToDirectory(new File(logs + SystemProperties.fileSeparator + clusterName + "_access.log"), logDest);
               }

               int majorVersion = Integer.parseInt(context.getVersion().split("\\.")[0]);
               String patternString = null;
               if (majorVersion > 2) {
                  patternString = clusterName + ".*\\.log\\.gz";
               } else {
                  patternString = clusterName + ".log.\\d{4}-\\d{2}-\\d{2}";
               }

               processLogVersions(patternString, maxLogs, logDir,logDest);

               patternString = "gc*.log.*";
               processLogVersions(patternString, maxGcLogs, logDir, logDest);

            }
         } else {
            logger.error("Configured log directory is not readable or does not exist: " + logDir.getAbsolutePath());
            context.setLocalAddressLocated(false);
         }

      } catch (Exception e) {
         logger.error("Error processing logs: Error encountered reading directory. Does the account you are running under have sufficient permissions to read the log directories?");
         logger.error("Log directory: " + logs);
      }

      logger.info("Finished processing logs.");


      return true;
   }

   private String determineLogLocation(String home, String log, String defaultLog) {

      String logLoc;

      if (!"".equals(log)) {
         logLoc = log;
      } else if ("".equals(log) && !"".equals(defaultLog)) {
         logLoc = home + SystemProperties.fileSeparator + "logs";
      } else {
         logLoc = defaultLog;
      }

      return logLoc;

   }

   private void processLogVersions(String pattern, int maxToGet, File logDir, File logDest) throws Exception{

      FileFilter logFileFilter = new RegexFileFilter(pattern);
      File[] logFileList = logDir.listFiles(logFileFilter);
      Arrays.sort(logFileList, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
      int limit = maxToGet, count = 0;
      for (File logfile : logFileList) {
         if (count < limit) {
            FileUtils.copyFileToDirectory(logfile, logDest);
            count++;
         } else {
            break;
         }
      }
   }


}
