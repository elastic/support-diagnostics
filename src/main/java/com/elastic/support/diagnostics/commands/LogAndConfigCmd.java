package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class LogAndConfigCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      if (context.getInputParams().isSkipLogs() || ! context.isLocalAddressLocated()) {
         return true;
      }

      JsonNode diagNode = context.getTypedAttribute("diagNode", JsonNode.class);
      if(diagNode == null){
         logger.error("Could not locate node running on current host.");
            return true;
      }

      boolean getAccess = context.getInputParams().isAccessLogs();
      String commercialDir = SystemUtils.safeToString(context.getAttribute("commercialDir"));
      logger.info("Processing logs and configuration files.");

      JsonNode settings = diagNode.path("settings");
      Iterator<JsonNode> inputArgs = diagNode.path("jvm").path("input_arguments").iterator();

      String name = diagNode.path("name").asText();
      context.setAttribute("diagNodeName", name);

      String clusterName = context.getClusterName();
      JsonNode nodePaths = settings.path("path");
      JsonNode defaultPaths = settings.path("default").path("path");

      String inputArgsConfig = findConfigArg(inputArgs);
      String config = nodePaths.path("config").asText();
      String logs = nodePaths.path("logs").asText();
      String conf = nodePaths.path("conf").asText();
      String home = nodePaths.path("home").asText();
      String defaultLogs = defaultPaths.path("logs").asText();
      String defaultConf = defaultPaths.path("conf").asText();
      String configFileLoc = "";

      try {
         List<String> fileDirs = new ArrayList<>();
         context.setAttribute("tempFileDirs", fileDirs);

         // Create a directory for this node
         String nodeDir = context.getTempDir() + SystemProperties.fileSeparator + name + Constants.logDir;
         fileDirs.add(nodeDir);

         Files.createDirectories(Paths.get(nodeDir));
         FileFilter configFilter = new WildcardFileFilter("*.yml");
         configFileLoc = determineConfigLocation(conf, config, home, defaultConf, inputArgsConfig);

         // Process the config directory
         String configDest = nodeDir + SystemProperties.fileSeparator + "config";
         File configDir = new File(configFileLoc);
         if(configDir.exists() ){

           FileUtils.copyDirectory(configDir, new File(configDest), configFilter, true);

            if (commercialDir != "") {
               File comm = new File(configFileLoc + SystemProperties.fileSeparator + commercialDir);
               if (comm.exists()) {
                  FileUtils.copyDirectory(comm, new File(configDest + SystemProperties.fileSeparator + commercialDir), true);
               }
            }

            File scripts = new File(configFileLoc + SystemProperties.fileSeparator + "scripts");
            if (scripts.exists()) {
               FileUtils.copyDirectory(scripts, new File(configDest + SystemProperties.fileSeparator + "scripts"), true);
            }
         }

         File logDest = new File(nodeDir + SystemProperties.fileSeparator + "logs");
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
               final Collection<File> gcLogs = FileUtils.listFiles(new File(logs), new WildcardFileFilter("gc*.log*"), TrueFileFilter.INSTANCE);
               for (final File gcLog : gcLogs) {
                  FileUtils.copyFileToDirectory(gcLog, logDest);
               }

               if (getAccess) {
                  FileUtils.copyFileToDirectory(new File(logs + SystemProperties.fileSeparator + clusterName + "_access.log"), logDest);
               }
               int majorVersion = Integer.parseInt(context.getVersion().split("\\.")[0]);
               String patternString = null;
               if (majorVersion > 2) {
                  patternString = clusterName + "-\\d{4}-\\d{2}-\\d{2}.log*";
               } else {
                  patternString = clusterName + ".log.\\d{4}-\\d{2}-\\d{2}";
               }
               // Get the two most recent server log rollovers
               //Pattern pattern = Pattern.compile(patternString);
               FileFilter logFilter = new RegexFileFilter(patternString);
               File[] logDirList = logDir.listFiles(logFilter);
               Arrays.sort(logDirList, LastModifiedFileComparator.LASTMODIFIED_REVERSE);

               int limit = 2, count = 0;
               for (File logListing : logDirList) {
                  if (count < limit) {
                     FileUtils.copyFileToDirectory(logListing, logDest);
                     count++;
                  } else {
                     break;
                  }
               }
            }
         }
         else {
            logger.error("Configured log directory is not readable or does not exist: " + logDir.getAbsolutePath());
            context.setLocalAddressLocated(false);
         }

      } catch (Exception e) {
         logger.error("Error processing log and config files: Error encountered reading directory. Does the account you are running under have sufficient permisssions to read the config and log directories?");
         logger.error("Log directory: " + logs + ",  config file location: " + configFileLoc);
      }

      logger.info("Finished processing logs and configuration files.");


      return true;
   }

   public String determineConfigLocation(String conf, String config, String home, String defaultConf, String inputArgsConfig) {

      String configFileLoc;

      //Check for the config location
      if (!"".equals(config)) {
         int idx = config.lastIndexOf(SystemProperties.fileSeparator);
         configFileLoc = config.substring(0, idx);
      } else if (!"".equals(conf)) {
         configFileLoc = conf;
      } else if ("".equals(conf) && !"".equals(defaultConf)) {
         configFileLoc = defaultConf;
      } else if (! "".equals(inputArgsConfig)){
         configFileLoc = inputArgsConfig;
      } else {
         configFileLoc = home + SystemProperties.fileSeparator + "config";
      }

      return configFileLoc;
   }

   String determineLogLocation(String home, String log, String defaultLog) {

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

   String findConfigArg(Iterator<JsonNode> args){

      try{
         while(args.hasNext()){
            String arg = args.next().asText();
            if(arg.contains("-Des.path.conf=")){
               return arg.replace("-Des.path.conf=", "");
            }
         }
      }
      catch (Exception e){
         logger.error("Error parsing input arguments for config directory:" + args);
      }

      return "";

   }
}
