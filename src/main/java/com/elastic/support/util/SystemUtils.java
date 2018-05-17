package com.elastic.support.util;


import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.converters.BooleanConverter;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemUtils {

   private static final Logger logger = LoggerFactory.getLogger(SystemUtils.class);

   public static StringUtils CommonsStringUtils;
   public static NumberUtils CommonsNumberUtils;
   public static org.springframework.util.StringUtils SpringStringUtils;
   public static PropertyUtils CommonsPropertyUtils;
   public static java.util.Objects JavaObjects;

   public static final String UTC_DATE_FORMAT = "MM/dd/yyyy KK:mm:ss a Z";
   public static final String FILE_DATE_FORMAT = "yyyyMMdd-KKmmssa";
   private static final String verTwo = "2(\\.\\d+)+";
   private static final String verX = "$major\\.$minor(\\.\\d)*";
   private static final String IPV4 = "(\\d+\\.\\d+\\.\\d+\\.\\d+\\:\\d+)";
   private static final String IPV6 = "^(:?[a-fA-F0-9]{0,4}:){1,7}[a-fA-F0-9]{1,4}%?[a-zA-Z0-9-_]{0,10}$";
   private static final DecimalFormat formatter = new DecimalFormat("###.##");
   private static final String EMPTY = "";
   public static final long GB = 1024 * 1024 * 1024;
   public static final long MB = 1024 * 1024;
   public static final long KB = 1024;
   public static final long sixtyFour = 65535;


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

   public static String formatDouble(double input) {
      return formatter.format(input);
   }

   public static long milliToSeconds(long millis) {
      return millis / 1000;
   }

   public static long milliToMinutes(long millis) {
      return (millis / 1000) / 60;
   }

   public static Object getProperty(Object target, String name) {

      Object result = null;
      try {
         result = PropertyUtils.getProperty(target, name);
      } catch (Exception e) {
         logger.error("Could not access property {} from target object", name, e);
      }

      return result;

   }

   public static Map getMapProperty(Object target, String name) {
      Object result = getProperty(target, name);
      if (result == null || !(result instanceof Map)) {
         return new LinkedHashMap();
      } else {
         return (Map) result;
      }
   }

   public static List getListProperty(Object target, String name) {
      Object result = getProperty(target, name);
      if (result == null || !(result instanceof List)) {
         return new ArrayList();
      } else {
         return (List) result;
      }
   }


   public static String getStringProperty(Object target, String name) {
      Object result = getProperty(target, name);
      return toString(result);
   }

   public static String getStringProperty(Object target, String name, String defaultValue) {
      Object result = getProperty(target, name);
      return toString(result, defaultValue);
   }

   public static Long getLongProperty(Object target, String name, Long defaultValue) {
      Object result = getProperty(target, name);
      return toLong(result, defaultValue);
   }

   public static Integer getIntProperty(Object target, String name, Integer defaultValue) {
      Object result = getProperty(target, name);
      return toInt(result, defaultValue);
   }

   public static Double getDoubleProperty(Object target, String name, Double defaultValue) {
      Object result = getProperty(target, name);
      return toDouble(result, defaultValue);
   }

   public static Boolean getBooleanProperty(Object target, String name, Boolean defaultValue) {
      Object result = getProperty(target, name);
      return toBoolean(result, defaultValue);
   }

   public static String toString(Object input) {
      return JavaObjects.toString(input, "");
   }

   public static String toString(Object input, String defaultValue) {
      if (defaultValue == null) {
         defaultValue = "";
      }
      return JavaObjects.toString(input, defaultValue);
   }

   public static Long toLong(Object input, long defaultValue) {
      if (input == null) return defaultValue;
      if (input instanceof Long) return (Long) input;
      return NumberUtils.toLong(toString(input), defaultValue);
   }

   public static Long toLong(Object input) {
      return toLong(input, -1);
   }

   public static Integer toInt(Object input, int defaultValue) {
      if (input == null) return defaultValue;
      if (input instanceof Integer) return (Integer) input;
      return NumberUtils.toInt(toString(input), defaultValue);
   }

   public static Integer toInt(Object input) {
      return toInt(input, -1);
   }

   public static Double toDouble(Object input, double defaultValue) {
      if (input == null) return defaultValue;
      if (input instanceof Double) return (Double) input;
      return NumberUtils.toDouble(toString(input), defaultValue);
   }

   public static Double toDouble(Object input) {
      return toDouble(input, -1.00);
   }

   public static Boolean toBoolean(Object input, boolean defaultValue) {
      if (input == null) return defaultValue;
      if (input instanceof Boolean) return (Boolean) input;
      BooleanConverter booleanConverter = new BooleanConverter(defaultValue);
      return booleanConverter.convert(null, input);
   }

   public static Boolean toBoolean(Object input) {
      return toBoolean(input, false);
   }

   protected static <T> T convert(Object val, Class<T> clazz) {

      if (val == null) return null;
      T castValue = null;
      String conversionError = "Value {} was converted to {}.";

      try {
         ConvertUtils.convert(val, clazz);
         castValue = clazz.cast(val);
      } catch (Exception e) {
         logger.error(conversionError, val, clazz.getName());
      }

      return castValue;

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

   public static String bytesToUnits(long size) {
      String hrSize = null;

      double b = size;
      double k = size / 1024.0;
      double m = ((size / 1024.0) / 1024.0);
      double g = (((size / 1024.0) / 1024.0) / 1024.0);
      double t = ((((size / 1024.0) / 1024.0) / 1024.0) / 1024.0);

      DecimalFormat dec = new DecimalFormat("0.00");

      if (t > 1) {
         hrSize = dec.format(t).concat(" TB");
      } else if (g > 1) {
         hrSize = dec.format(g).concat(" GB");
      } else if (m > 1) {
         hrSize = dec.format(m).concat(" MB");
      } else if (k > 1) {
         hrSize = dec.format(k).concat(" KB");
      } else {
         hrSize = dec.format(b).concat(" Bytes");
      }

      return hrSize;
   }

   public static boolean streamClose(String path, InputStream instream) {

      if (instream != null) {
         try {
            instream.close();
         } catch (Throwable t) {
            logger.error("Error encountered when attempting to close file {}", path);
            return false;
         }
      } else {
         logger.error("Error encountered when attempting to close file: null InputStream {}", path);
      }

      return true;

   }

   public static String extract(String filename, String dir) {

      String diagOutput = "";

      try {
         final int BUFFER = 2048;
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
               if (initial) {
                  diagOutput = fl.substring(0, fl.length() - 1);
                  initial = false;
               }
            } else {
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

   public static void createFileAppender(String logDir, String logFile) {

      logDir = logDir + SystemProperties.fileSeparator + logFile;

      final LoggerContext context = (LoggerContext) LogManager.getContext(false);
      final Configuration config = context.getConfiguration();
      /*Layout layout = PatternLayout.createLayout("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n", null, config, null,
         null,true, true, null, null );*/
      Layout layout = PatternLayout.newBuilder()
         .withConfiguration(config)
         .withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")
         .build();

      Appender appender = FileAppender.newBuilder().setConfiguration(config)
         .withFileName(logDir)
         .withAppend(false)
         .withLocking(false)
         .withName("File")
         .withImmediateFlush(true)
         .withIgnoreExceptions(false)
         .withBufferedIo(false)
         .withBufferSize(0)
         .withLayout(layout)
         .withAdvertise(false).build();

      appender.start();
      config.addAppender(appender);
      AppenderRef.createAppenderRef("File", null, null);
      config.getRootLogger().addAppender(appender, null, null);
      context.updateLoggers();

   }

   public static void cleanup(String dir) {

      try {
         LoggerContext lc = (LoggerContext) LogManager.getContext(false);
         final Configuration config = lc.getConfiguration();
         Appender appndr = config.getAppender("File");
         appndr.stop();
         config.getRootLogger().removeAppender("File");
         File tempdir = new File(dir);
         tempdir.setWritable(true, false);
         FileUtils.deleteDirectory(tempdir);
      } catch (IOException e) {
         String msg = "Error deleting temporary work directory";
         logger.error(msg, e);
      }

      logger.info("Temp directory {} was deleted.", dir);

   }

}