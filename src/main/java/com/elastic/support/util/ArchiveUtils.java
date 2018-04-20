package com.elastic.support.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class ArchiveUtils {

   private static final Logger logger = LogManager.getLogger();

   public static String extractFileFromTargz(TarArchiveInputStream archive) {

      BufferedReader br = null;
      StringBuffer sb = new StringBuffer();
      try {
         TarArchiveEntry tae = archive.getNextTarEntry();
         while (tae != null) {
            String name = tae.getName();
            logger.debug("Entry name " + name);

               long sz = tae.getSize();
/*               if (sz > 0) {
                  br = new BufferedReader(new InputStreamReader(archive));
                  String thisLine = null;
                  while ((thisLine = br.readLine()) != null) {
                     sb.append(thisLine);
                  }

                  String output = sb.toString();
                  return output;
               }*/

            tae = archive.getNextTarEntry();
         }
      } catch (IOException e) {
         logger.error("Error extracting {}.", "", e);
         throw new RuntimeException("Error extracting {} from archive.", e);
      } finally {
         try {
            br.close();
         } catch (IOException e) {
            logger.error("Failed to close stream reader.");
         }
      }

      return sb.toString();

   }

    public static TarArchiveInputStream readArchive(String archivePath) throws Exception {

      FileInputStream fileStream = new FileInputStream(archivePath);
      BufferedInputStream inStream = new BufferedInputStream(fileStream);
      TarArchiveInputStream tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(inStream));
      return tarIn;

   }

}

