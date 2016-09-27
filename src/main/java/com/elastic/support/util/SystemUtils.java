package com.elastic.support.util;


import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemUtils {

   private static final Logger logger = LoggerFactory.getLogger(SystemUtils.class);

   public static final String UTC_DATE_FORMAT = "MM/dd/yyyy KK:mm:ss a Z";
   public static final String FILE_DATE_FORMAT = "yyyyMMdd-KKmmssa";
   private static final String verTwo = "2(\\.\\d+)+";
   private static final String verX = "$major\\.$minor(\\.\\d)*";
   private static final String IPV4 = "(\\d+\\.\\d+\\.\\d+\\.\\d+\\:\\d+)";
   private static final String IPV6 = "^(:?[a-fA-F0-9]{0,4}:){1,7}[a-fA-F0-9]{1,4}%?[a-zA-Z0-9-_]{0,10}$";
   private static final DecimalFormat formatter = new DecimalFormat("###.##");
   private static final String EMPTY = "";
   public static final long GB = 1024*1024*1024;
   public static final long MB = 1024* 1024;
   public static final long KB = 1024;
   public static final long sixtyFour= 65535;


   public static String getUtcDateString() {
      Date curDate = new Date();
      SimpleDateFormat format = new SimpleDateFormat(UTC_DATE_FORMAT);
      return format.format(curDate);
   }

   public static String getFileDateString() {
      Date curDate = new Date();
      SimpleDateFormat format = new SimpleDateFormat(FILE_DATE_FORMAT);
      return format.format(curDate);
   }

   public static boolean isVersionTwoOrGreater(String version) {
      return Pattern.matches(verTwo, version);
   }

   public static boolean isVersion(String major, String minor, String version) {
      String pattern = verX;
      pattern = verX.replace("$major", major).replace("$minor", minor);
      return Pattern.matches(pattern, version);
   }

   public static String milliToHourMinuteString(long millis) {
      long uptimeHours = millis / 3600000;
      long uptimeMinutes = millis % 3600000;
      return uptimeHours + "hours " + uptimeMinutes + " minutes";
   }

   public static String formatDouble(double input){
      return formatter.format(input);
   }

   public static long milliToSeconds(long millis) {
      return millis / 1000;
   }

   public static long milliToMinutes(long millis) {
      return (millis / 1000) / 60;
   }

   public static String safeToString(Object input) {
      return input == null ? "" : input.toString();
   }

   public static String emptyDisplayString(Object input){

      String res = SystemUtils.safeToString(input);
      if (res.equals("")){
         return EMPTY;
      }
      else{
         return res;
      }
   }

   public static long safeToLong(Object input) {

      String ret = input == null ? "0" : input.toString();

      try {
         long res = new Long(input.toString());
         return res;
      } catch (Exception e) {
         logger.error("Could not convert: " + input + " to long value.");
      }

      return -1;
   }

   public static int safeToInt(Object input) {

      String ret = input == null ? "0" : input.toString();

      try {
         int res = new Integer(input.toString());
         return res;
      } catch (Exception e) {
         logger.error("Could not convert: " + input + " to long value.");
      }

      return -1;
   }

   public static double safeToDouble(Object input) {

      String ret = input == null ? "0" : input.toString();

      try {
         double res = new Double(input.toString());
         return res;
      } catch (Exception e) {
         logger.error("Could not convert: " + input + " to long value.");
      }

      return -1;
   }


   public static String extractIpPort(String address) {

      Pattern patternIpv4 = Pattern.compile(IPV4);
      Pattern patternIpv6 = Pattern.compile(IPV6);

      Matcher matcher4 = patternIpv4.matcher(address);
      if (matcher4.find()) {
         return matcher4.group(0);
      }

      Matcher matcher6 = patternIpv6.matcher(address);
      if (matcher6.find()) {
         return matcher6.group(0);
      }

      logger.warn("The following string containing an IP Address could not be parsed.  Displaying as read. " + address);
      return address;

   }

   public static String bytesToUnits(long byteSize){

      if(byteSize / GB > 1){
         return byteSize / GB + "GB";
      }
      else if(byteSize / MB > 1){
         return byteSize / MB + "MB";
      }
      else if(byteSize / KB  > 1){
         return  byteSize / KB + "KB";
      }

      else return byteSize + "b";

   }

   public static String extract(String filename, String dir){

      String diagOutput = "";

      try {
         final  int BUFFER = 2048;
         FileInputStream fin = new FileInputStream(filename);
         BufferedInputStream in = new BufferedInputStream(fin);
         GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
         TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn);
         TarArchiveEntry entry = null;

         boolean initial = true;
         while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
            System.out.println("Extracting: " + entry.getName());

            if (entry.isDirectory()) {
               String fl = dir + SystemProperties.fileSeparator + entry.getName();
               File f = new File(fl);
               f.mkdirs();
               if (initial){
                  diagOutput = fl.substring(0, fl.length()-1);
                  initial = false;
               }
            }
            else {
               int count;
               byte data[] = new byte[BUFFER];
               FileOutputStream fos = new FileOutputStream(dir
                  + SystemProperties.fileSeparator + entry.getName());

               BufferedOutputStream dest = new BufferedOutputStream(fos,
                  BUFFER);

               while ((count = tarIn.read(data, 0, BUFFER)) != -1) {
                  dest.write(data, 0, count);
               }
               dest.close();
            }
         }

         tarIn.close();

      } catch (IOException e) {
         logger.error("Error extracting diagnostic archive: " + e);
      }

      System.out.println("untar completed successfully!!");
      return diagOutput;

   }
}