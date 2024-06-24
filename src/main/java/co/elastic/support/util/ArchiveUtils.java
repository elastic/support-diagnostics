/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.util;

import co.elastic.support.Constants;
import co.elastic.support.diagnostics.DiagnosticException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;

public class ArchiveUtils {

   private static final Logger logger = LogManager.getLogger(ArchiveUtils.class);

   public static File createZipArchive(String dir, String archiveFileName) throws DiagnosticException {
      File srcDir = new File(dir);
      String filename = dir + "-" + archiveFileName + ".zip";
      File file = new File(filename);

      try (
            FileOutputStream fout = new FileOutputStream(filename);
            ZipArchiveOutputStream taos = new ZipArchiveOutputStream(fout)) {
         archiveResultsZip(archiveFileName, taos, srcDir, null, true);
         logger.info(Constants.CONSOLE, "Archive: " + filename + " was created");
         return file;
      } catch (IOException ioe) {
         throw new DiagnosticException("Couldn't create zip archive.", ioe);
      }
   }

   private static void archiveResultsZip(
         String archiveFilename,
         ZipArchiveOutputStream zipFileStream,
         File file,
         String path,
         boolean append) {
      String relPath = (path == null ? "" : path + "/") + file.getName();

      try {
         if (append) {
            relPath += "-" + archiveFilename;
         }

         zipFileStream.putArchiveEntry(new ZipArchiveEntry(file, relPath));

         if (file.isFile()) {
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
               IOUtils.copy(bis, zipFileStream);
               zipFileStream.closeArchiveEntry();
            }
         } else if (file.isDirectory()) {
            zipFileStream.closeArchiveEntry();
            for (File childFile : file.listFiles()) {
               archiveResultsZip(archiveFilename, zipFileStream, childFile, relPath, false);
            }
         }
      } catch (IOException e) {
         logger.error(Constants.CONSOLE, "Archive Error", e);
      }
   }

   public static void extractArchive(String filename, String targetDir) throws IOException {
      ArchiveInputStream ais = null;
      try {
         InputStream inputStream = new FileInputStream(new File(filename));
         if (filename.endsWith(".zip")) {
            ais = new ZipArchiveInputStream(inputStream);
         } else {
            logger.error(Constants.CONSOLE, "Unsupported archive type");
            return;
         }

         ArchiveEntry entry = ais.getNextEntry();
         String archiveDir = entry.getName();
         while ((entry = ais.getNextEntry()) != null) {
            String newPath = entry.getName().replace(archiveDir, "");
            if (newPath.endsWith(".zip") || newPath.endsWith(".tar") | newPath.endsWith("tar.gz")) {
               String nestedArch = entryToDisk(ais, targetDir, newPath);
               String nestedTargetDir = nestedArch.substring(nestedArch.lastIndexOf(SystemProperties.fileSeparator));
               extractArchive(nestedArch, nestedTargetDir);
               new File(nestedArch).delete();
            } else if (entry.isDirectory()) {
               File f = new File(targetDir + SystemProperties.fileSeparator + newPath);
               boolean created = f.mkdir();
               if (!created) {
                  System.out.printf("Unable to create directory '%s', during extraction of archive contents.\n",
                        f.getAbsolutePath());
               }
            } else {
               entryToDisk(ais, targetDir, newPath);
               /*
                * int count;
                * byte data[] = new byte[bufferSize];
                * FileOutputStream fos = new FileOutputStream(new File(targetDir +
                * SystemProperties.fileSeparator + newPath), false);
                * try (BufferedOutputStream dest = new BufferedOutputStream(fos, bufferSize)) {
                * while ((count = ais.read(data, 0, bufferSize)) != -1) {
                * dest.write(data, 0, count);
                * }
                * }
                */
            }
         }

         System.out.println("Extract completed successfully!");
      } catch (IOException e) {
         logger.error(e);
      } finally {
         if (ais != null) {
            ais.close();
         }
      }
   }

   private static String entryToDisk(ArchiveInputStream ais, String targetDir, String newPath) throws IOException {
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
