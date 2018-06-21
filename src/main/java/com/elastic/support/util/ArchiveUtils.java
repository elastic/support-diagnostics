package com.elastic.support.util;

import com.elastic.support.diagnostics.PostProcessor;
import com.elastic.support.diagnostics.PostProcessorBypass;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;


public class ArchiveUtils {

   private static final Logger logger = LogManager.getLogger();
   PostProcessor postProcessor = new PostProcessorBypass();

   public ArchiveUtils(PostProcessor scrubberUtils){
      super();
      this.postProcessor = scrubberUtils;
   }

   public ArchiveUtils(){
      super();
   }

   public void createArchive(String dir, String archiveFileName) {

      try {

         File srcDir = new File(dir);
         String filename = dir + "-" + archiveFileName + ".tar.gz";

         FileOutputStream fout = new FileOutputStream(filename);
         CompressorOutputStream cout = new GzipCompressorOutputStream(fout);
         TarArchiveOutputStream taos = new TarArchiveOutputStream(cout);

         taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
         taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
         archiveResults(archiveFileName, taos, srcDir, "", true);
         taos.close();

         logger.info("Archive: " + filename + " was created");

      } catch (Exception ioe) {
         logger.error("Couldn't create archive. {}", ioe);
      }

   }

   public void archiveResults(String archiveFilename, TarArchiveOutputStream taos, File file, String path, boolean append) {

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

   public void extractDiagnosticArchive(String sourceInput, String targetDir) throws Exception {
      extractDiagnosticArchive(sourceInput, targetDir, false);
   }

   public void extractDiagnosticArchive(String sourceInput, String targetDir, boolean scrub) throws Exception {

      Files.createDirectories(Paths.get(targetDir + SystemProperties.fileSeparator + "logs"));
      TarArchiveInputStream archive = readDiagArchive(sourceInput);

      String baseArchivePath = null;
      logger.info("Extracting archive...");

      try {
         // Base archive name
         TarArchiveEntry tae = archive.getNextTarEntry();
         baseArchivePath = tae.getName();

         // First actual archived entry
         tae = archive.getNextTarEntry();

         while (tae != null) {
            String name = tae.getName();
            name = name.replace(baseArchivePath, "");

            if(name.contains("gc.log.")){
               logger.info("Processing: {}", name);
               name = name.replace("logs/", "");
               processEntry(archive, name + ".log", targetDir + SystemProperties.fileSeparator + "logs", false);
            }
            else if (name.endsWith(".gz")) {
               logger.info("Processing: {}", name);
               name = name.replace(".gz", "");
               name = name.replace("logs/", "");
               GZIPInputStream zip = new GZIPInputStream(archive);
               processEntry(zip, name, targetDir + SystemProperties.fileSeparator + "logs", false);
            }
            else if (name.contains(".log") && !name.equalsIgnoreCase("diagnostics.log")) {
               logger.info("Processing: {}", name);
               name = name.replace("logs/", "");
               processEntry(archive, name, targetDir + SystemProperties.fileSeparator + "logs", true);
             }
             else if( ! (name.contains( "logs" + SystemProperties.fileSeparator )) && ! (name.contains( "log" + SystemProperties.fileSeparator ))) {
               logger.info("Processing: {}", name);
               processEntry(archive, name, targetDir, true);
            }

            tae = archive.getNextTarEntry();
         }
      } catch (IOException e)      {
         logger.error("Error extracting {}.", "", e);
         throw new RuntimeException("Error extracting {} from archive.", e);
      }
   }

   private void processEntry (InputStream ais, String name, String targetDir, boolean scrub) throws Exception {

      try {
         BufferedReader br = null;
         BufferedWriter writer = new BufferedWriter(new FileWriter(
            targetDir + SystemProperties.fileSeparator + name));
         br = new BufferedReader(new InputStreamReader(ais));
         String thisLine = null;
         while ((thisLine = br.readLine()) != null) {
            thisLine = postProcessor.process(thisLine);
            writer.write(thisLine);
            writer.newLine();
         }

         writer.close();
      } catch (Throwable t) {
         logger.error("Error processing entry,", t);
      }

   }

   private TarArchiveInputStream readDiagArchive(String archivePath) throws Exception {

      FileInputStream fileStream = new FileInputStream(archivePath);
      BufferedInputStream inStream = new BufferedInputStream(fileStream);
      TarArchiveInputStream tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(inStream));
      return tarIn;

   }


}

