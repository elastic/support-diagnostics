package com.elastic.support.diagnostics;

import com.beust.jcommander.JCommander;
import com.elastic.support.diagnostics.chain.DiagnosticChainExec;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Diagnostic {

   private InputParams inputs = new InputParams();
   private DiagnosticChainExec dc = new DiagnosticChainExec();
   private DiagnosticContext ctx = new DiagnosticContext(inputs);
   private Logger logger = LogManager.getLogger();

   public Diagnostic(String args[]) {

      logger.info("Validating inputs...");
      ctx.setInputParams(inputs);
      JCommander jc = new JCommander(inputs);
      jc.setCaseSensitiveOptions(true);

      try {
         jc.parse(args);
         if (!validateAuth(inputs)) {
            throw new RuntimeException("Inputs error: If authenticating both username and password are required.");
         }
         // Set up the output directory
         logger.info("Creating temp directory.");
         createOutputDir(ctx);
         logger.info("Created temp directory: {}", ctx.getTempDir());

         // Start logging to file
         createFileAppender(ctx);
         logger.info("Configuring log file.");

      } catch (RuntimeException re) {
         logger.error("Error during diagnostic initialization: {}", re.getMessage());
         jc.usage();
         return;
      } catch (Exception e) {
         logger.error("Error during diagnostic initialization", e);
         return;
      }

      if (inputs.isHelp()) {
         jc.usage();
         return;
      }

   }

   public void exec(){

      try {
        int reps = inputs.getReps();
         long interval = inputs.getInterval() * 1000;

         if (reps > 1) {
            for (int i = 1; i <= reps; i++) {
               ctx.setCurrentRep(i);
               if (inputs.getDiagType().equalsIgnoreCase(Constants.STANDARD_DIAG) && i < (reps)) {
                  inputs.setSkipLogs(true);
               }
               else{
                  inputs.setSkipLogs(false);
               }
               dc.runDiagnostic(ctx);
               System.out.println("Run " + i + " of " + reps + " completed.");
               if (i < reps) {
                  logger.info("Next run will occur in " + inputs.getInterval() + " seconds.\n");
                  Thread.sleep(interval);
               }
            }
         }
         else {
            dc.runDiagnostic(ctx);
         }

      } catch (Exception re) {
         logger.error("Execution Error", re);
      }
      finally{
         createArchive(ctx);
         cleanup(ctx);
      }
   }

   private  boolean validateAuth(InputParams inputs) {

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

      if (! diagType.equals(Constants.ES_DIAG)) {
         context.setDiagName(diagType + "-" + Constants.ES_DIAG);
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

   private void createFileAppender(DiagnosticContext ctx) {

      String logDir = ctx.getTempDir() + SystemProperties.fileSeparator + "diagnostics.log";

      final LoggerContext context = (LoggerContext) LogManager.getContext(false);
      final Configuration config = context.getConfiguration();
      Layout layout = PatternLayout.createLayout("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n", null, config, null,
         null,true, true, null, null );
      Appender appender = FileAppender.createAppender(logDir, "false", "false", "File", "true",
         "false", "false", "0", layout, null, "false", null, config);
      appender.start();
      config.addAppender(appender);
      AppenderRef ref = AppenderRef.createAppenderRef("File", null, null);
      AppenderRef[] refs = new AppenderRef[] {ref};

      config.getRootLogger().addAppender(appender, null, null);
      context.updateLoggers();
      Logger testLog = LogManager.getLogger();
      testLog.error("Testing");
   }

   private void createArchive(DiagnosticContext context) {

      logger.info("Archiving diagnostic results.");

      try {
         String archiveFilename = SystemProperties.getFileDateString();
         if(context.getInputParams().getReps() > 1){
            int currentRep = context.getCurrentRep();
            if(currentRep == 1){
               context.setAttribute("archiveFileName", archiveFilename);
            }

            archiveFilename = context.getStringAttribute("archiveFileName") + "-run-" + currentRep;
         }

         boolean bzip = context.getInputParams().isBzip();
         String ext = "";
         if (bzip){
            ext = ".bz2";
         }
         else{
            ext = ".gz";
         }

         String dir = context.getTempDir();
         File srcDir = new File(dir);
         String filename = dir + "-" + archiveFilename + ".tar" + ext;

         FileOutputStream fout = new FileOutputStream(filename);
         CompressorOutputStream cout = null;
         if(bzip){
            cout = new BZip2CompressorOutputStream(fout);
         }
         else {
            cout = new GzipCompressorOutputStream(fout);
         }
         TarArchiveOutputStream taos = new TarArchiveOutputStream(cout);

         taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
         taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
         archiveResults(archiveFilename, taos, srcDir, "", true);
         taos.close();

         logger.info("Archive: " + filename + " was created");

      } catch (Exception ioe) {
         logger.error("Couldn't create archive. {}", ioe);
      }

   }

   private void archiveResults(String archiveFilename, TarArchiveOutputStream taos, File file, String path, boolean append) {

      boolean pathSet = false;
      String relPath = "";

      try {
         if (append) {
            relPath = path + "/" + file.getName() + "-" + archiveFilename;
         } else {
            relPath = path + "/" + file.getName();
         }
         TarArchiveEntry tae = new TarArchiveEntry(file, relPath);
         taos.putArchiveEntry(tae);

         if (file.isFile()) {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            IOUtils.copy(bis, taos);
            taos.closeArchiveEntry();
            bis.close();

         } else if (file.isDirectory()) {
            taos.closeArchiveEntry();
            for (File childFile : file.listFiles()) {
               archiveResults(archiveFilename, taos, childFile, relPath, false);
            }
         }
      } catch (IOException e) {
         logger.error("Archive Error", e);
      }
   }

   private void cleanup(DiagnosticContext context) {

      String dir = context.getTempDir();
      try {
         FileUtils.deleteDirectory(new File(dir));
      } catch (IOException e) {
         String msg = "Error deleting temporary work directory";
         logger.error(msg, e);
      }

      logger.info("Temp directory {} was deleted.", dir);


   }
}
