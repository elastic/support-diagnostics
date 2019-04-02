package com.elastic.support.config;

public class Constants {

   public static final String ES_DIAG_DEFAULT = "standard";
   public static final String ES_DIAG = "diagnostics";
   public static final String NOT_FOUND = "not found";

   public static final String DIAG_CONFIG = "diags.yml";
   public static final String CHAINS_CONFIG = "chains.yml";

   public static final String NODES = "nodes.json";
   public static final String DIAG_VERSION = "diagVersion";

   public static final int DEEFAULT_HTTP_PORT = 80;
   public static final int DEEFAULT_HTTPS_PORT = 443;

   public static final int LOGSTASH_PORT = 9600;
   public static final String LOCAL_ADDRESSES = "127.0.0.1;localhost;::1";

   public static final String UTF8 = "UTF8";

   public static final String IPv6Regex =
      "(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))";
   public static final String IPv4Regex = "((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
   public static final String MacAddrRegex = "([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})?";

}
