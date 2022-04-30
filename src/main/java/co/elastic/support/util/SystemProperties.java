/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.util;


import org.apache.logging.log4j.Level;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SystemProperties {

   public static Level DIAG = Level.forName("DIAG", 250);
   public static Level CONSOLE = Level.forName("CONSOLE", 50);

   public static final String osName = System.getProperty("os.name");

   public static final String javaHome = System.getenv("JAVA_HOME");

   public static final String pathSeparator = System.getProperty("path.separator");

   public static final String fileSeparator = System.getProperty("file.separator");

   public static final String lineSeparator = System.getProperty("line.separator");

   public static final String userDir = System.getProperty("user.dir");

   public static final String userHome = System.getProperty("user.home");

   public static final String UTC_DATE_FORMAT = "yyyy-MM-dd";

   public static final String UTC_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

   public static final String FILE_DATE_FORMAT = "yyyyMMdd-HHmmss";

   public static String getUtcDateString() {
      Date curDate = new Date();
      SimpleDateFormat format = new SimpleDateFormat(UTC_DATE_FORMAT);
      format.setTimeZone(TimeZone.getTimeZone("UTC"));
      return format.format(curDate);
   }

   public static String getUtcDateTimeString() {
      Date curDate = new Date();
      SimpleDateFormat format = new SimpleDateFormat(UTC_DATE_TIME_FORMAT);
      format.setTimeZone(TimeZone.getTimeZone("UTC"));
      return format.format(curDate);
   }

   public static String getFileDateString() {
      Date curDate = new Date();
      SimpleDateFormat format = new SimpleDateFormat(FILE_DATE_FORMAT);
      format.setTimeZone(TimeZone.getTimeZone("UTC"));
      return format.format(curDate);
   }
}
