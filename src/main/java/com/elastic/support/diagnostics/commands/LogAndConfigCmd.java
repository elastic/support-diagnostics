package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogAndConfigCmd extends AbstractDiagnosticCmd {

   private static final String NODES = "nodes.json";
   private static final String ACCESS = "access";

   public boolean execute(DiagnosticContext context) {

      if (context.getInputParams().isSkipLogs()) {
         return true;
      }

      boolean getAccess = context.getInputParams().isAccessLogs();

      logger.info("Processing logs and configuration files.");
      try {
         Set hosts = context.getHostIpList();

         // Get the nodes info:
         String temp = context.getTempDir();
         JsonNode rootNode = JsonYamlUtils.createJsonNodeFromFileName(temp, NODES);
         boolean needPid = true;
         JsonNode nodes = rootNode.path("nodes");
         Iterator<JsonNode> it = nodes.iterator();
         List<String> fileDirs = new ArrayList<>();
         context.setAttribute("tempFileDirs", fileDirs);

         while (it.hasNext()) {
            JsonNode n = it.next();

            String host = n.path("host").asText();
            String httpPublish = n.path("httpPublish").asText();
            String transportPublish = n.path("transportPublish").asText();

            // if the host we're on doesn't match up with the node entry
            // then bypass it and move to the next node
            if (hosts.contains(host) || hosts.contains(httpPublish) || hosts.contains(transportPublish)) {
               String name = n.path("name").asText();

               JsonNode settings = n.path("settings");
               JsonNode nodePaths = settings.path("path");
               JsonNode defaultPaths = settings.path("default").path("path");

               String config = nodePaths.path("config").asText();
               String logs = nodePaths.path("logs").asText();
               String conf = nodePaths.path("conf").asText();
               String home = nodePaths.path("home").asText();
               String defaultLogs = defaultPaths.path("logs").asText();
               String defaultConf = defaultPaths.path("conf").asText();;

               if (needPid) {
                  JsonNode jnode = n.path("process");
                  String pid = jnode.path("id").asText();
                  context.setPid(pid);
               }

               // Create a directory for this node
               String nodeDir = context.getTempDir() + SystemProperties.fileSeparator + name + Constants.logDir;
               fileDirs.add(nodeDir);

               Files.createDirectories(Paths.get(nodeDir));
               FileFilter configFilter = new WildcardFileFilter("*.yml");
               String configFileLoc = determineConfigLocation(conf, config, home, defaultConf);
               logs = determineLogLocation(home, logs, defaultLogs);

               // Copy the config directory
               String configDest = nodeDir + SystemProperties.fileSeparator + "config";
               FileUtils.copyDirectory(new File(configFileLoc), new File(configDest), configFilter, true);

               File shield = new File(configFileLoc + SystemProperties.fileSeparator + "shield");
               if (shield.exists()) {
                  FileUtils.copyDirectory(shield, new File(configDest + SystemProperties.fileSeparator + "shield"), true);
               }

               File scripts = new File(configFileLoc + SystemProperties.fileSeparator + "scripts");
               if (scripts.exists()) {
                  FileUtils.copyDirectory(scripts, new File(configDest + SystemProperties.fileSeparator + "scripts"), true);
               }

               String logPattern = "*.log";
               if (context.getInputParams().isArchivedLogs()) {
                  logPattern = "*.*";
               }

               File logDir = new File(logs);
               File logDest = new File(nodeDir + SystemProperties.fileSeparator + "logs");

               FileFilter logFilter = new WildcardFileFilter(logPattern);
               File[] logDirList = logDir.listFiles(logFilter);

               String patternString = ".*\\d{4}-\\d{2}-\\d{2}.log";
               Pattern pattern = Pattern.compile(patternString);

               for (File logListing : logDirList) {
                  String filename = logListing.getName();
                  Matcher matcher = pattern.matcher(filename);
                  boolean matches = matcher.matches();
                  if (matches) {
                     continue;
                  }

                  if (logListing.getName().contains(ACCESS)) {
                     if (!getAccess) {
                        continue;
                     }
                  }
                  FileUtils.copyFileToDirectory(logListing, logDest);
               }

               logger.info("Processed logs and configs for node: " + name);
            }

         }
      } catch (Exception e) {
         logger.error("Error processing log and config files.", e);
      }

      logger.info("Finished processing logs and configuration files.");

      return true;
   }

   public String determineConfigLocation(String conf, String config, String home, String defaultConf) {

      String configFileLoc;

      //Check for the config location
      if (!"".equals(config)) {
         int idx = config.lastIndexOf(SystemProperties.fileSeparator);
         configFileLoc = config.substring(0, idx);
      } else if (! "".equals(conf)) {
         configFileLoc = conf;
      } else if ("".equals(conf) && ! "".equals(defaultConf)) {
         configFileLoc = defaultConf;
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
}
