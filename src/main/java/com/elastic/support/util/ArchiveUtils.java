package com.elastic.support.util;

import com.elastic.support.Constants;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Enumeration;


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
      if(! createZipArchive(dir, archiveFileName)){
         logger.error(Constants.CONSOLE,  "Couldn't create zip archive. Trying tar.gz");
         if(! createTarArchive(dir, archiveFileName)){
            logger.info(Constants.CONSOLE, "Couldn't create tar.gz archive.");
         }
      }
   }

   public boolean createZipArchive(String dir, String archiveFileName)  {

      try {
         File srcDir = new File(dir);
         String filename = dir + "-" + archiveFileName + ".zip";

         FileOutputStream fout = new FileOutputStream(filename);
         ZipArchiveOutputStream taos = new ZipArchiveOutputStream(fout);
         archiveResultsZip(archiveFileName, taos, srcDir, "", true);
         taos.close();

         logger.info(Constants.CONSOLE, "Archive: " + filename + " was created");

      } catch (Exception ioe) {
         logger.error( "Couldn't create archive.", ioe);
         return false;
      }
      return true;

   }

   public boolean createTarArchive(String dir, String archiveFileName) {

      try {
         File srcDir = new File(dir);
         String filename = dir + "-" + archiveFileName + ".tar.gz";

         FileOutputStream fout = new FileOutputStream(filename);
         CompressorOutputStream cout = new GzipCompressorOutputStream(fout);
         TarArchiveOutputStream taos = new TarArchiveOutputStream(cout);

         taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
         taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
         archiveResultsTar(archiveFileName, taos, srcDir, "", true);
         taos.close();

         logger.info(Constants.CONSOLE,  "Archive: " + filename + " was created");

      } catch (Exception ioe) {
         logger.error( "Couldn't create archive.", ioe);
         return false;
      }

      return true;

   }

   public void archiveResultsZip(String archiveFilename, ZipArchiveOutputStream taos, File file, String path, boolean append) {
      String relPath = "";

      try {
         if (append) {
            relPath = path + "/" + file.getName() + "-" + archiveFilename;
         } else {
            relPath = path + "/" + file.getName();
         }
         ZipArchiveEntry tae = new ZipArchiveEntry(file, relPath);
         taos.putArchiveEntry(tae);

         if (file.isFile()) {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            IOUtils.copy(bis, taos);
            taos.closeArchiveEntry();
            bis.close();

         } else if (file.isDirectory()) {
            taos.closeArchiveEntry();
            for (File childFile : file.listFiles()) {
               archiveResultsZip(archiveFilename, taos, childFile, relPath, false);
            }
         }
      } catch (IOException e) {
         logger.error(Constants.CONSOLE,"Archive Error", e);
      }
   }

   public void archiveResultsTar(String archiveFilename, TarArchiveOutputStream taos, File file, String path, boolean append) {
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
               archiveResultsTar(archiveFilename, taos, childFile, relPath, false);
            }
         }
      } catch (IOException e) {
         logger.error(Constants.CONSOLE,"Archive Error", e);
      }
   }

   public void extractDiagnosticArchive(String sourceInput) throws Exception {

      logger.info(Constants.CONSOLE,  "Extracting archive...");

      try {
         ZipFile zf = new ZipFile(new File(sourceInput));
         archiveProcessor.init(zf);

         Enumeration<ZipArchiveEntry> entries = zf.getEntriesInPhysicalOrder();
         while(entries.hasMoreElements()){
            ZipArchiveEntry tae = entries.nextElement();
            String name = tae.getName();
            int fileStart = name.indexOf("/");
            name = name.substring(fileStart + 1);
            archiveProcessor.process(zf.getInputStream(tae), name);
         }

      } catch (IOException e)      {
         logger.error(Constants.CONSOLE, "Error extracting {}.", "", e);
         throw new RuntimeException("Error extracting {} from archive.", e);
      }
   }
}



