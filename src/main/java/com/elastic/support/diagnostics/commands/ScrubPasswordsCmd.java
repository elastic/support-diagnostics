package com.elastic.support.diagnostics.commands;

import com.elastic.support.util.SystemProperties;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.JsonYamlUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScrubPasswordsCmd extends AbstractDiagnosticCmd {

   private static final String configFile = "elasticsearch.yml";

   public boolean execute(DiagnosticContext context) {

      if (! context.isLocalAddressLocated() || ! context.getInputParams().isScrubFiles()) {
         logger.info("Password redaction not configured.");
         return true;
      }

      Map diagConfig = context.getConfig();
      List<String> passwordKeys = (List<String>)diagConfig.get("password-keys");

      logger.info("Password redaction: elasticsearch config.");

      List<String> tempFileDirs = (List<String>)context.getAttribute("tempFileDirs");

      try {

         for(String temp: tempFileDirs) {
            // Get the nodes info:

            File dir = new File(temp + SystemProperties.fileSeparator + "config" );
            //FileFilter fileFilter = new RegexFileFilter("\\*.yml");
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
              if (!files[i].isDirectory() && files[i].getName().contains(".yml")) {
                 redactConfig(files[i].getAbsolutePath(), passwordKeys);
                 logger.info("Scrubbed passwords for " + files[i].getAbsolutePath());
               }
               System.out.println(files[i]);
            }
         }

      } catch (Exception e) {
         logger.error("Error redacting config passwords.", e);
         logger.error("Password removal failed - please examine archive to ensure sensitive information is removed.");      }

      logger.info("Finished processing logs and configuration files.");

      return true;
   }

   public void redactConfig(String conf, List<String> passwordKeys) throws Exception {

      Map<String, Object> cfg = JsonYamlUtils.readYamlFromPath(conf, true);

      if(cfg == null || cfg.size() == 0){
         logger.warn("No configured properties were present in: " + conf + ".  This is unusual for a production system and may indicate misconfiguration." );
         return;
      }

      Map<String, Object> flatCfg = JsonYamlUtils.flattenYaml(cfg);

      Set<String> keys = flatCfg.keySet();
      for (String key: keys){
         for(String passwordKey: passwordKeys){
            if (key.contains(passwordKey)){
               flatCfg.put(key, "*********");
            }
         }
      }

      FileUtils.deleteQuietly(new File(conf));
      JsonYamlUtils.writeYaml(conf, flatCfg);

   }
}