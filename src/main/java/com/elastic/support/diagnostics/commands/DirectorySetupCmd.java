package com.elastic.support.diagnostics.commands;

import com.elastic.support.SystemProperties;
import com.elastic.support.diagnostics.DiagnosticContext;
import com.elastic.support.diagnostics.InputParams;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DirectorySetupCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      // Set up where we want to put the results - it may come in from the command line
      String outputDir = setOutputDir(context.getInputParams());
      context.setOutputDir(outputDir);
      logger.info("Results will be written to: " + outputDir);
      String tempDir = outputDir + SystemProperties.fileSeparator + "diagnostics";
      context.setTempDir(tempDir);

      // Create the temp directory - delete if first if it exists from a previous run
      try {
         FileUtils.deleteDirectory(new File(tempDir));
         Files.createDirectories(Paths.get(tempDir));
      } catch (IOException e) {
         logger.error("Temp dir could not be created", e);
         throw new RuntimeException("Could not create temp directory - see logs for details.");
      }

      logger.debug("Created temp directory: " + tempDir);
      System.out.println("Creating " + tempDir + " as temporary directory.\n");

      return true;
   }

   public String setOutputDir(InputParams inputs) {

      if ("cwd".equalsIgnoreCase(inputs.getOutputDir())) {
         return SystemProperties.userDir;
      } else {
         return inputs.getOutputDir();
      }
   }

}
