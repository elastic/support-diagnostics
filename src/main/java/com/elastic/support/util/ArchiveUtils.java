package com.elastic.support.util;

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


public class ArchiveUtils {

   private static final Logger logger = LogManager.getLogger(ArchiveUtils.class);
   ArchiveEntryProcessor archiveProcessor;

   public ArchiveUtils(ArchiveEntryProcessor processor){
      super();
      this.archiveProcessor = processor;
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

   public void extractDiagnosticArchive(String sourceInput) throws Exception {

      TarArchiveInputStream archive = readDiagArchive(sourceInput);
      logger.info("Extracting archive...");

      try {
         // Base archive name - it's not redundant like Intellij is complaining...
         TarArchiveEntry tae = archive.getNextTarEntry();

         // First actual archived entry
         tae = archive.getNextTarEntry();

         while (tae != null) {
            String name = tae.getName();
            int fileStart = name.indexOf("/");
            name = name.substring(fileStart + 1);
            archiveProcessor.process(archive, name);
            tae = archive.getNextTarEntry();
         }
      } catch (IOException e)      {
         logger.error("Error extracting {}.", "", e);
         throw new RuntimeException("Error extracting {} from archive.", e);
      }
   }


   private TarArchiveInputStream readDiagArchive(String archivePath) throws Exception {

      FileInputStream fileStream = new FileInputStream(archivePath);
      BufferedInputStream inStream = new BufferedInputStream(fileStream);
      TarArchiveInputStream tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(inStream));
      return tarIn;

   }


}

