package com.elastic.support;

import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constants {

   public static final Marker CONSOLE = MarkerManager.getMarker("CONSOLE");

   public static final String ES_DIAG = "diagnostics";
   public static final String ES_DIAG_TEMP = "diagnostics-temp";
   public static final String NOT_FOUND = "not found";
   public static final String CHECK_LOG = "Check diagnostics.log in the archive file for more detail.";

   public static final String DIAG_CONFIG = "diags.yml";
   public static final String ES_REST = "elastic-rest.yml";
   public static final String LS_REST = "logstash-rest.yml";
   public static final String MONITORING_REST = "monitoring-rest.yml";

   public static final String QUERY_CONFIG_PACKAGE = "monitoring-extract/";
   public static final String TEMPLATE_CONFIG_PACKAGE = "monitoring-extract/templates/";
   public static final String MONITORING_DIR = "monitoring-export";

   public static final String DIAG_VERSION = "diagVersion";
   public static final String[] LOCAL_ADDRESSES = {"127.0.0.1","localhost", "[::1]"};
   public static final List<String> localAddressList = Arrays.asList(LOCAL_ADDRESSES);
   public static final String UTF_8 = "UTF-8";
   public static final String UTF_16 = "UTF-16";
   public static final String[] exePaths = {"/bin/","/usr/bin/","/usr/sbin/","/usr/local/bin/","/usr/local/sbin/"};

   public static final String IPv6Regex =
      "(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))";
   public static final String IPv4Regex = "((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
   public static final String MacAddrRegex = "([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})?";
   public static final String timeZoneRegex = "^([+-](?:2[0-3]|[01][0-9]):[0-5][0-9])$";

   public static final String winPlatform = "winOS";
   public static final String linuxPlatform = "linuxOS";
   public static final String macPlatform = "macOS";

   public final static String local =  "local";
   public final static String api = "api";
   public final static String cloud = "cloud";
   public final static String remote = "remote";

   public final static String logstashLocal = "logstash-local";
   public final static String logstashRemote = "logstash-remote";
   public final static String logstashApi = "logstash-api";

   public static final String runningInIde = "runningInIde";
   public static final String interactiveMsg = "Command line options can be displayed with the --help argument. " +
           "Entering interactive mode.";

}
