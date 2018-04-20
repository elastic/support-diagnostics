package com.elastic.support.diagnostics;

import com.beust.jcommander.JCommander;
import com.elastic.support.util.ArchiveUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Scrubber {

   private static final Logger logger = LogManager.getLogger();
   private ScrubInputParams inputs = new ScrubInputParams();

   public Scrubber(String args[]) {

      logger.info("Validating inputs...");
      JCommander jc = new JCommander(inputs);
      jc.setCaseSensitiveOptions(true);

      try {
         jc.parse(args);
         // Unzip the current diagnostic


      } catch (RuntimeException re) {
         logger.error("Error during diagnostic initialization: {}", re.getMessage());
         jc.usage();
         return;
      } catch (Exception e) {
         logger.error("Error during diagnostic initialization", e);
         return;
      }

   }

   public void exec() throws Exception{
      String archivePath = inputs.getArchive();
      TarArchiveInputStream tais = ArchiveUtils.readArchive(archivePath);
      ArchiveUtils.extractFileFromTargz(tais);




   }



}