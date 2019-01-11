package com.elastic.support.diagnostics;

import com.beust.jcommander.JCommander;
import com.elastic.support.diagnostics.chain.DiagnosticChainExec;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.ArchiveUtils;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Diagnostic {

   private InputParams inputs = new InputParams();
   private DiagnosticChainExec dc = new DiagnosticChainExec();
   private DiagnosticContext ctx = new DiagnosticContext();
   private Logger logger = LogManager.getLogger();
   private boolean proceedToRun = false;

   Diagnostic(String args[]) {

      logger.info("Validating inputs...");
      ctx.setInputParams(inputs);
      JCommander jc = new JCommander(inputs);
      jc.setCaseSensitiveOptions(true);

      try {
         jc.parse(args);

         if (inputs.isHelp()) {
            jc.usage();
            proceedToRun = false;
            return;
         }

         if (!inputs.getDiagType().equalsIgnoreCase(Constants.LOCAL_DIAG)) {
            if (StringUtils.isEmpty(inputs.getHost())) {
               throw new RuntimeException("Inputs error: You must enter the hostname of a running node within the cluster, preferably on the host you are running the diagnostic from.");
            }
         }

         if (!validateAuth(inputs)) {
            throw new RuntimeException("Inputs error: If authenticating both username and password are required.");
         }

         // Set up the output directory
         logger.info("Creating temp directory.");
         createOutputDir(ctx);
         logger.info("Created temp directory: {}", ctx.getTempDir());

         // Start logging to file
         SystemUtils.createFileAppender(ctx.getTempDir(), "diagnostics.log");
         logger.info("Configuring log file.");

      } catch (RuntimeException re) {
         logger.info("Error during diagnostic initialization: {}", re.getMessage());
         jc.usage();
         return;
      } catch (Exception e) {
         logger.info("Error during diagnostic initialization", e);
         return;
      }

      proceedToRun = true;

   }

   public boolean isProceedToRun() {
      return proceedToRun;
   }

   void exec() {

      try {
         int reps = inputs.getReps();
         long interval = inputs.getInterval() * 1000;

         if (reps > 1) {
            for (int i = 1; i <= reps; i++) {
               ctx.setCurrentRep(i);
               if (inputs.getDiagType().equalsIgnoreCase(Constants.STANDARD_DIAG) && i < (reps)) {
                  inputs.setNoLogs(true);
               } else {
                  inputs.setNoLogs(false);
               }
               dc.runDiagnostic(ctx);
               System.out.println("Run " + i + " of " + reps + " completed.");
               if (i < reps) {
                  logger.info("Next run will occur in " + inputs.getInterval() + " seconds.\n");
                  Thread.sleep(interval);
               }
            }
         } else {
            dc.runDiagnostic(ctx);
         }

      } catch (Exception re) {
         logger.error("Execution Error", re);
      } finally {
         createArchive(ctx);
         SystemUtils.cleanup(ctx.getTempDir());
      }
   }

   private boolean validateAuth(InputParams inputs) {

      String ptPassword = inputs.getPlainTextPassword();
      String userName = inputs.getUsername();
      String password = inputs.getPassword();
      if (!"".equals(ptPassword)) {
         password = ptPassword;
         inputs.setPassword(ptPassword);
      }

      return !((userName != null && password == null) || (password != null && userName == null));
   }

   private void createOutputDir(DiagnosticContext context) {

      // Set up where we want to put the results - it may come in from the command line
      String outputDir = formatOutputDir(context.getInputParams());
      context.setOutputDir(outputDir);
      logger.info("Results will be written to: " + outputDir);
      String diagType = context.getInputParams().getDiagType();

      if (!diagType.equals(Constants.ES_DIAG_DEFAULT)) {
         context.setDiagName(diagType + "-" + Constants.ES_DIAG);
      } else {
         context.setDiagName(Constants.ES_DIAG);
      }

      String tempDir = outputDir + SystemProperties.fileSeparator + context.getDiagName();
      context.setTempDir(tempDir);

      // Create the temp directory - delete if first if it exists from a previous run
      try {
         FileUtils.deleteDirectory(new File(tempDir));
         Files.createDirectories(Paths.get(tempDir));
      } catch (IOException e) {
         logger.error("Temp dir could not be created", e);
         throw new RuntimeException("Could not create temp directory - see logs for details.");
      }

      logger.info("Creating {} as temporary directory.", tempDir);

   }

   private String formatOutputDir(InputParams inputs) {

      if ("cwd".equalsIgnoreCase(inputs.getOutputDir())) {
         return SystemProperties.userDir;
      } else {
         return inputs.getOutputDir();
      }
   }

   private void createArchive(DiagnosticContext context) {

      logger.info("Archiving diagnostic results.");

      try {
         String archiveFilename = SystemProperties.getFileDateString();
         if (context.getInputParams().getReps() > 1) {
            int currentRep = context.getCurrentRep();
            if (currentRep == 1) {
               context.setAttribute("archiveFileName", archiveFilename);
            }

            archiveFilename = context.getStringAttribute("archiveFileName") + "-run-" + currentRep;
         }

         String dir = context.getTempDir();
         ArchiveUtils archiveUtils = new ArchiveUtils();
         archiveUtils.createArchive(dir, archiveFilename);

      } catch (Exception ioe) {
         logger.error("Couldn't create archive. {}", ioe);
      }

   }

}
