package com.elastic.support.util;


import com.elastic.support.SystemProperties;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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


}