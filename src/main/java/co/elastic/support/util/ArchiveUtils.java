/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.util;

import co.elastic.support.Constants;
import co.elastic.support.diagnostics.DiagnosticException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;

public class ArchiveUtils {

   public enum ArchiveType {
      ZIP, TAR, ANY;

      public static ArchiveType fromString(String name) {
         for (ArchiveType type : ArchiveType.values()) {
            if (type.name().equalsIgnoreCase(name)) {
               return type;
            }
         }

         return ANY;
      }
   }

   private static final Logger logger = LogManager.getLogger(ArchiveUtils.class);

   public static File createArchive(String dir, String archiveFileName, ArchiveType type) throws DiagnosticException {
      switch (type) {
         case ZIP:
            try {
               return createZipArchive(dir, archiveFileName);
            } catch (IOException ioe) {
               throw new DiagnosticException("Couldn't create zip archive.", ioe);
            }
         case TAR:
            try {
               return createTarArchive(dir, archiveFileName);
            } catch (IOException ioe) {
               throw new DiagnosticException("Couldn't create tar.gz archive.", ioe);
            }
         default:
            try {
               return createZipArchive(dir, archiveFileName);
            } catch (IOException zipException) {
               logger.info(Constants.CONSOLE, "Couldn't create zip archive. Trying tar.gz");

               try {
                  return createTarArchive(dir, archiveFileName);
               } catch (Exception tarException) {
                  logger.info(Constants.CONSOLE, "Couldn't create tar.gz archive.");
                  tarException.addSuppressed(zipException);
                  throw new DiagnosticException("Couldn't create zip and tar.gz archives.", tarException);
               }
            }
      }
   }

   private static File createZipArchive(String dir, String archiveFileName) throws IOException {
      File srcDir = new File(dir);
      String filename = dir + "-" + archiveFileName + ".zip";
      File file = new File(filename);

      try (
         FileOutputStream fout = new FileOutputStream(filename);
         ZipArchiveOutputStream taos = new ZipArchiveOutputStream(fout)
      ) {
         archiveResultsZip(archiveFileName, taos, srcDir, "", true);
         logger.info(Constants.CONSOLE, "Archive: " + filename + " was created");
         return file;
      }
   }

   private static File createTarArchive(String dir, String archiveFileName) throws IOException {
      File srcDir = new File(dir);
      String filename = dir + "-" + archiveFileName + ".tar.gz";
      File file = new File(filename);

      try (
         FileOutputStream fout = new FileOutputStream(filename);
         CompressorOutputStream cout = new GzipCompressorOutputStream(fout);
         TarArchiveOutputStream taos = new TarArchiveOutputStream(cout)
      ) {
         taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
         taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
         archiveResultsTar(archiveFileName, taos, srcDir, "", true);

         logger.info(Constants.CONSOLE,  "Archive: " + filename + " was created");

         return file;
      }
   }

   private static void archiveResultsZip(String archiveFilename, ZipArchiveOutputStream taos, File file, String path, boolean append) {
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
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
               IOUtils.copy(bis, taos);
               taos.closeArchiveEntry();
            }
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

   private static void archiveResultsTar(String archiveFilename, TarArchiveOutputStream taos, File file, String path, boolean append) {
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
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
               IOUtils.copy(bis, taos);
               taos.closeArchiveEntry();
            }
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

   public static void extractArchive(String filename, String targetDir) throws IOException {
      final int bufferSize = 1024;
      ArchiveInputStream ais = null;
      try  {
         InputStream inputStream  = new FileInputStream(new File(filename));
         if(filename.endsWith(".zip")){
            ais = new ZipArchiveInputStream(inputStream);
         }
         else if (filename.endsWith(".tar")){
            ais = new TarArchiveInputStream(inputStream);
         }
         else if (filename.endsWith(".tar.gz")){
            ais = new TarArchiveInputStream(new GzipCompressorInputStream(inputStream));
         }
         else{
            logger.error(Constants.CONSOLE, "Unsupported archive type");
            return;
         }

         ArchiveEntry entry = ais.getNextEntry();
         String archiveDir = entry.getName();
         while (( entry = ais.getNextEntry()) != null) {
            String newPath = entry.getName().replace(archiveDir, "");
            if(newPath.endsWith(".zip") || newPath.endsWith(".tar") | newPath.endsWith("tar.gz")){
               String nestedArch = entryToDisk(ais, targetDir, newPath);
               String nestedTargetDir = nestedArch.substring(nestedArch.lastIndexOf(SystemProperties.fileSeparator));
               extractArchive(nestedArch, nestedTargetDir);
               new File(nestedArch).delete();
            }
            else if (entry.isDirectory()) {
               File f = new File(targetDir + SystemProperties.fileSeparator + newPath);
               boolean created = f.mkdir();
               if (!created) {
                  System.out.printf("Unable to create directory '%s', during extraction of archive contents.\n",
                          f.getAbsolutePath());
               }
            } else {
               entryToDisk(ais, targetDir, newPath);
               /*int count;
               byte data[] = new byte[bufferSize];
               FileOutputStream fos = new FileOutputStream(new File(targetDir + SystemProperties.fileSeparator + newPath), false);
               try (BufferedOutputStream dest = new BufferedOutputStream(fos, bufferSize)) {
                  while ((count = ais.read(data, 0, bufferSize)) != -1) {
                     dest.write(data, 0, count);
                  }
               }*/
            }
         }

         System.out.println("Extract completed successfully!");
      } catch (IOException e) {
         logger.error(e);
      }
      finally {
         if (ais != null) {
            ais.close();
         }
      }
   }

   private static String entryToDisk(ArchiveInputStream ais, String targetDir, String newPath) throws IOException{
      int bufferSize = 1024;
      int count;
      byte data[] = new byte[bufferSize];
      String fileName = targetDir + SystemProperties.fileSeparator + newPath;
      FileOutputStream fos = new FileOutputStream(new File(fileName), false);
      try (BufferedOutputStream dest = new BufferedOutputStream(fos, bufferSize)) {
         while ((count = ais.read(data, 0, bufferSize)) != -1) {
            dest.write(data, 0, count);
         }
      }
      return fileName;
   }

}


