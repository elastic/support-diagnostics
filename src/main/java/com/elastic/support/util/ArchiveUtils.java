package com.elastic.support.util;

import com.elastic.support.Constants;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;


public class ArchiveUtils {

   private static final Logger logger = LogManager.getLogger(ArchiveUtils.class);

   public static void archiveDirectory(String srcDir, String archiveName){
      try (ZipArchiveOutputStream archive = new ZipArchiveOutputStream(new FileOutputStream(archiveName))) {

         File folderToZip = new File(srcDir);

         // Walk through files, folders & sub-folders.
         Files.walk(folderToZip.toPath()).forEach(p -> {
            File file = p.toFile();
            if (!file.isDirectory()) {
               String relativePath = file.getPath().replace(srcDir, "");
               logger.debug("Zipping file - {} into archive as {}", file, relativePath);
               ZipArchiveEntry entry_1 = new ZipArchiveEntry(file, relativePath);
               try (FileInputStream fis = new FileInputStream(file)) {
                  archive.putArchiveEntry(entry_1);
                  IOUtils.copy(fis, archive);
                  archive.closeArchiveEntry();
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
         });
         archive.finish();
      } catch (IOException e) {
         logger.error("Error creating zip from temp dir", e);
         throw new RuntimeException("Could not create zip archive.");
      }
   }

/*
   public static boolean createZipArchive(String tempDir, String archiveFileName)  {

      try {
         File srcDir = new File(tempDir);
         FileOutputStream fout = new FileOutputStream(archiveFileName);
         ZipArchiveOutputStream taos = new ZipArchiveOutputStream(fout);
         archiveResultsZip(archiveFileName, taos, srcDir, "", true);
         taos.close();

         logger.info(Constants.CONSOLE, "Archive: " + archiveFileName + " was created");

      } catch (Exception ioe) {
         logger.error( "Couldn't create archive.", ioe);
         return false;
      }
      return true;

   }

   public static void archiveResultsZip(String archiveFilename, ZipArchiveOutputStream zaos, File file, String path, boolean append) {
      String relPath = "";

      try {
         if (append) {
            //relPath = path + "/" + file.getName() + "-" + archiveFilename;
            relPath = file.getName() + "-" + archiveFilename;
         } else {
            relPath = path + "/" + file.getName();
         }
         ZipArchiveEntry tae = new ZipArchiveEntry(file, relPath);
         zaos.putArchiveEntry(tae);

         if (file.isFile()) {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            IOUtils.copy(bis, zaos);
            zaos.closeArchiveEntry();
            bis.close();

         } else if (file.isDirectory()) {
            zaos.closeArchiveEntry();
            for (File childFile : file.listFiles()) {
               archiveResultsZip(archiveFilename, zaos, childFile, relPath, false);
            }
         }
      } catch (IOException e) {
         logger.error(Constants.CONSOLE,"Archive Error", e);
      }
   }

   public static void archiveResultsTar(String archiveFilename, TarArchiveOutputStream taos, File file, String path, boolean append) {
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
*/

   public static void extractArchive(String filename, String targetDir) throws Exception{
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

         System.out.println("Data extracted from archive.");
      } catch (IOException e) {
         logger.error(e);
      }
      finally {
         ais.close();
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



