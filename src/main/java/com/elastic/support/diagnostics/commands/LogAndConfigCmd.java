package com.elastic.support.diagnostics.commands;

import com.elastic.support.SystemProperties;
import com.elastic.support.diagnostics.DiagnosticContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;

public class LogAndConfigCmd extends AbstractDiagnosticCmd{

    public boolean execute(DiagnosticContext context){

        try {
            Set hosts = context.getHostIpList();
            String manifestString = context.getManifest();
            boolean needPid = true;

            JsonNode rootNode = new ObjectMapper().readTree(manifestString);
            JsonNode nodes = rootNode.path("nodes");
            Iterator<JsonNode> it = nodes.iterator();

            while (it.hasNext()) {
                JsonNode n = it.next();
                String host = n.path("host").asText();
                String httpPublish = n.path("httpPublish").asText();
                String transportPublish = n.path("transportPublish").asText();

                // if the host we're on doesn't match up with the node entry
                // then bypass it and move to the next node
                if(hosts.contains(host) || hosts.contains(httpPublish) || hosts.contains(transportPublish)) {
                    String name = n.path("name").asText();
                    String config = n.path("config").asText();
                    String conf = n.path("conf").asText();
                    String logs = n.path("logs").asText();
                    String home = n.path("home").asText();

                    if(needPid){
                        String pid = n.path("pid").asText();
                        context.setPid(pid);
                    }

                    // Create a directory for this node
                    String nodeDir = context.getTempDir() + SystemProperties.fileSeparator + name + " node-log and config";

                    Files.createDirectories(Paths.get(nodeDir));
                    FileFilter configFilter = new WildcardFileFilter("*.yml");
                    String configFileLoc = determineConfigLocation(conf, config, home);

                    // Copy the config directory
                    String configDest = nodeDir + SystemProperties.fileSeparator + "config";
                    FileUtils.copyDirectory(new File(configFileLoc), new File(configDest), configFilter, true);

                    File shield = new File(configFileLoc + SystemProperties.fileSeparator + "shield");
                    if (shield.exists()){
                        FileUtils.copyDirectory(shield, new File(configDest + SystemProperties.fileSeparator + "shield"), true);
                    }

                    File scripts = new File(configFileLoc + SystemProperties.fileSeparator + "scripts");
                    if (scripts.exists()){
                        FileUtils.copyDirectory(scripts, new File(configDest + SystemProperties.fileSeparator + "scripts"), true);
                    }

                    if ("".equals(logs)) {
                        logs = home + SystemProperties.fileSeparator + "logs";
                    }

                    String logPattern = "*.log";
                    if(context.getInputParams().isArchivedLogs()) {
                        logPattern = "*.*";
                    }

                    File logDir = new File(logs);
                    File logDest = new File(nodeDir + SystemProperties.fileSeparator + "logs");

                    FileFilter logFilter = new WildcardFileFilter(logPattern);
                    FileUtils.copyDirectory(logDir, logDest, logFilter, true);

                    logger.debug("processed node:\n" + name);
                }

            }
        } catch (Exception e) {
            logger.error("Error processing the nodes manifest:\n", e);
            context.addMessage("An issue was encountered determining the location of the log and configuration files.");
        }

        return  true;
    }

    public String determineConfigLocation(String conf, String config, String home) {

        String configFileLoc;

        //Check for the config location
        if (!"".equals(config)) {
            int idx = config.lastIndexOf(SystemProperties.fileSeparator);
            configFileLoc = config.substring(0, idx);

        } else if (!"".equals(conf)) {
            configFileLoc = conf ;
        } else {
            configFileLoc = home + SystemProperties.fileSeparator + "config";
        }

        return configFileLoc;
    }
}
