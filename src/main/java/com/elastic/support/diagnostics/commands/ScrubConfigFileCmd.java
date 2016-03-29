package com.elastic.support.diagnostics.commands;

import com.elastic.support.SystemProperties;
import com.elastic.support.diagnostics.DiagnosticContext;
import com.elastic.support.util.JsonYamlUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileFilter;
import java.util.Map;
import java.util.Set;

public class ScrubConfigFileCmd extends AbstractDiagnosticCmd {

   private static final String logDirPattern = ".*-log and config";
   private static final String configFile = "elasticsearch.yml";

   public boolean execute(DiagnosticContext context) {

      logger.info("Scrubbing elasticsearch config.");
      try {

         // Get the nodes info:
         String temp = context.getTempDir();

         File dir = new File(temp);
         FileFilter fileFilter = new RegexFileFilter(logDirPattern);
         File[] files = dir.listFiles(fileFilter);
         for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
               String configPath = files[i].getAbsolutePath() + SystemProperties.fileSeparator + "config" + SystemProperties.fileSeparator + configFile;
               redactConfig(configPath);
               logger.info("Scrubbed passwords for " + configPath);
            }

            System.out.println(files[i]);
         }


      } catch (Exception e) {
         logger.error("Error redacting config passwords.", e);
         throw (new RuntimeException(e));
      }

      logger.info("Finished processing logs and configuration files.");

      return true;
   }

   public void redactConfig(String conf) throws Exception {

      Map<String, Object> cfg = JsonYamlUtils.readYamlFromPath(conf, true);
      Map<String, Object> flatCfg = JsonYamlUtils.flattenYaml(cfg);

      Set<String> keys = flatCfg.keySet();
      for (String key: keys){
         if (key.contains("access") || key.contains("password") || key.contains("secret")){
            flatCfg.put(key, "*********");
         }
      }

      FileUtils.deleteQuietly(new File(conf));
      JsonYamlUtils.writeYaml(conf, flatCfg);

   }
}