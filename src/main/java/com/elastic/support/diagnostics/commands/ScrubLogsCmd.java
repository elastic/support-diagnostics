package com.elastic.support.diagnostics.commands;

import com.elastic.support.util.SystemProperties;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.JsonYamlUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.*;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ScrubLogsCmd extends AbstractDiagnosticCmd {

   private static final String logDirPattern = ".*-log and config";
   private static final String logFilePattern = "*.log";
   private static final String configFile = "elasticsearch.yml";

   public boolean execute(DiagnosticContext context) {

      logger.info("Scrubbing elasticsearch logs and configuration using scrub.yml.");
      try {

         boolean scrubFiles = context.getInputParams().isScrubFiles();
         if(! scrubFiles){
            return true;
         }

         Map<String, Object> dictionary = JsonYamlUtils.readYamlFromClasspath("scrub.yml", false);
         if (dictionary.size() == 0){
            logger.warn("Scrubbing was enabled but no substitutions were defined. Bypassing log and configuration file processing.");
            return true;
         }

         WildcardFileFilter wcfFilter = new WildcardFileFilter(logFilePattern);
         // Get the nodes info:
         String temp = context.getTempDir();

         File dir = new File(temp);
         FileFilter fileFilter = new RegexFileFilter(logDirPattern);
         File[] files = dir.listFiles(fileFilter);
         for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
               String configPath = files[i].getAbsolutePath() + SystemProperties.fileSeparator + "config" +SystemProperties.fileSeparator + "elasticsearch.yml";
               File cfgFile = new File(configPath);
               scrubFile(cfgFile, dictionary);

               String logPath = files[i].getAbsolutePath() + SystemProperties.fileSeparator + "logs";
               File logDir = new File(logPath);
               Collection<File> logs = FileUtils.listFiles(logDir, wcfFilter, null);
               for(File logfile: logs){
                  scrubFile(logfile, dictionary);
               }

               logger.info("Processed scrubbed contents of " + logPath);
            }

            System.out.println(files[i]);
         }


      } catch (Exception e) {
         logger.error("Error scrubbing log and config files.", e);
         throw (new RuntimeException(e));
      }

      logger.info("Finished scrubbing logs and configs.");

      return true;
   }

   private  void scrubFile(File file, Map<String, Object> dictionary) throws Exception{
      String filepath = file.getAbsolutePath();
      String filename = file.getName();
      String scrubbedFilename = filepath.replaceAll(filename, "scrubbed-"+ filename);

      Set<Map.Entry<String, Object>> entries = dictionary.entrySet();

      BufferedReader br = new BufferedReader(new FileReader(file));
      BufferedWriter bw = new BufferedWriter((
         new FileWriter(scrubbedFilename)));

      String line = br.readLine();
      while(line != null){
         for(Map.Entry entry: entries ){
            line = line.replaceAll(entry.getKey().toString(), entry.getValue().toString());
         }
         bw.write(line);
         bw.newLine();
         line = br.readLine();
      }

      br.close();
      bw.flush();
      bw.close();

      FileUtils.deleteQuietly(file);

   }

}