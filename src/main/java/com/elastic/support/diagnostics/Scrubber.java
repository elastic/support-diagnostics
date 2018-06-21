package com.elastic.support.diagnostics;
import com.beust.jcommander.JCommander;
import com.elastic.support.util.ArchiveUtils;
import com.elastic.support.util.ScrubberUtils;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Scrubber {

   private static final Logger logger = LogManager.getLogger();

   private ScrubInputParams inputs = new ScrubInputParams();
   private boolean proceedToRun = true;

   public Scrubber(String args[]) {

      logger.info("Validating inputs...");
      JCommander jc = new JCommander(inputs);
      jc.setCaseSensitiveOptions(true);

      try {
         jc.parse(args);
         if (inputs.isHelp()) {
            jc.usage();
            proceedToRun = false;
            return;
         }
      } catch (RuntimeException re) {
         logger.error("Error during diagnostic initialization: {}", re.getMessage());
         jc.usage();
      } catch (Exception e) {
         logger.error("Error during diagnostic initialization", e);
      }

   }

   public boolean isProceedToRun() {
      return proceedToRun;
   }

   public void exec() throws Exception{

      String archivePath = inputs.getArchive();
      String targetDir = inputs.getTargetDir();

      int pos = archivePath.lastIndexOf(SystemProperties.fileSeparator);

      if(targetDir == null){
         targetDir = archivePath.substring(0, pos) + SystemProperties.fileSeparator + "scrubbed";
      }
      else{
         targetDir = targetDir +  SystemProperties.fileSeparator + "scrubbed";
      }

      SystemUtils.createFileAppender(targetDir, "scrubber.log");
      String scrubbedName = (archivePath.substring(pos + 1)).replace(".tar.gz", "");
      ArchiveUtils archiveUtils = new ArchiveUtils(new ScrubberUtils(inputs.getScrubFile()));
      archiveUtils.extractDiagnosticArchive(archivePath, targetDir );
      archiveUtils.createArchive(targetDir, scrubbedName);
      SystemUtils.cleanup(targetDir);

   }

}