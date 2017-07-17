package com.elastic.support.diagnostics;

public class Constants {

   public static final String ES_DIAG = "diagnostics";
   public static final String LOGSTASH_DIAG = "logstash";
   public static final String REMOTE_DIAG = "remote";
   public static final String STANDARD_DIAG = "standard";

   public static final int LOGSTASH_PORT = 9600;
   public static final int HTTP_GET = 1;
   public static final int HTTP_POST = 2;
   public static final int HTTP_PUT = 3;
   public static final int HTTP_DELETE = 4;

   public static final String logDir = "_log_config";
   public static final String logDirPattern = "*_log_config";
   public static final String logFilePattern = "[a-zA-Z0-9]+$(?!-\\d\\d\\d\\d-\\d\\d-\\d\\d$)*.log";
   public static final String configFile = "elasticsearch.yml";

}