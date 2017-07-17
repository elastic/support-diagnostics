package com.elastic.support.diagnostics;

import com.beust.jcommander.*;

public class InputParams {

   @Parameter(names = {"-?", "--help"}, description = "Help contents.", help = true)
   private boolean help;

   @Parameter(names = {"-o", "--out", "--output", "--outputDir"}, description = "Fully qualified path to output directory or c for current working directory.")
   private String outputDir = "cwd";

   @Parameter(names = {"-h", "--host",}, required = true, description = "Required field.  Hostname, IP Address, or localhost.  HTTP access must be enabled.")
   private String host = "";

   @Parameter(names = {"--port"}, description = "HTTP or HTTPS listening port. Defaults to 9200.")
   private int port = 9200;

   @Parameter(names = {"-u", "--user"}, description = "Username.")
   private String username;

   @Parameter(names = {"-p", "--password"}, description = "Password", password = true)
   private String password;

   @Parameter(names = {"-s", "--ssl", "--https"}, description = "Use SSL?  No value required, only the option.")
   private boolean isSsl = false;

   @Parameter(names = {"--type"}, description ="Diagnostic type to run. Enter standard, remote, logstash. Default is standard. Using remote will suppress retrieval of logs, configuration and system command info.")
   private String diagType = "standard";

   @Parameter(names = {"--ptp"}, description = "Insecure plain text password - warning, may exposure access.")
   private String plainTextPassword = "";

   @Parameter(names = {"--reps"}, description = "Number of times to execute the diagnostic. Use to create multiple runs at timed intervals.")
   private int reps = 1;

   @Parameter(names = {"--interval"}, description = "Elapsed time in seconds between diagnostic runs when in repeating mode.")
   private long interval = 30;

   @Parameter(names = {"--bzip"}, description = "Set this option true to use bzip istead of gzip.")
   private boolean isBzip = false;

   @Parameter(names = {"--archivedLogs"}, description = "Get archived logs in addition to current ones if present - No value required, only the option.")
   private boolean archivedLogs=false;

   @Parameter(names = {"--scrub"}, description = "Set to true to use the scrub.yml dictionary to scrub logs and config files.  See README for more info.")
   private boolean scrubFiles = false;

   @Parameter(names = {"--noVerify"}, description = "Use this option to bypass hostname verification for certificate. This is inherently unsafe and NOT recommended.")
   private boolean skipVerification = false;

   @Parameter(names= {"--keystore"}, description = "Keystore for client certificate.")
   private String keystore;

   @Parameter(names= {"--keystorePass"}, description = "Keystore password for client certificate.")
   private String keystorePass;

   @Parameter(names = {"--noLogs"}, description = "Use this option to suppress log collection.")
   private boolean skipLogs = false;

   @Parameter(names = {"--accessLogs"}, description = "Use this option to collect access logs as well.")
   private boolean accessLogs = false;

   @Parameter(names = {"--threads"}, description = "Collect only hot threads.")
   private boolean hotThreads = false;

   private boolean secured = false;
   private boolean wasPortSet = false;

   public boolean isHotThreads() {
      return hotThreads;
   }

   public void setHotThreads(boolean hotThreads) {
      this.hotThreads = hotThreads;
   }

   public boolean isSkipLogs() {
      return skipLogs;
   }

   public void setSkipLogs(boolean skipLogs) {
      this.skipLogs = skipLogs;
   }

   public String getKeystore() {
      return keystore;
   }

   public void setKeystore(String keystore) {
      this.keystore = keystore;
   }

   public String getKeystorePass() {
      return keystorePass;
   }

   public void setKeystorePass(String keystorePass) {
      this.keystorePass = keystorePass;
   }

   public boolean isSkipVerification() {
      return skipVerification;
   }

   public void setSkipVerification(boolean skipVerification) {
      this.skipVerification = skipVerification;
   }

   public boolean isScrubFiles() {
      return scrubFiles;
   }

   public void setScrubFiles(boolean scrubFiles){
      this.scrubFiles = scrubFiles;
   }

   public String getHost() {
      return host;
   }

   public void setHost(String host) {
      this.host = host;
   }

   public String getDiagType() {
      return diagType;
   }

   public void setDiagType(String diagType) {
      this.diagType = diagType;
   }

   public int getPort() {
      if(diagType.equalsIgnoreCase("logstash")){
         if( this.port == 9200 ){
            return Constants.LOGSTASH_PORT;
         }
      }
      return port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getOutputDir() {
      return outputDir;
   }

   public void setOutputDir(String outputDir) {
      this.outputDir = outputDir;
   }

   public boolean isSecured() {
      return (this.username != null && this.password != null);
   }

   public boolean isSsl() {
      return isSsl;
   }

   public void setIsSsl(boolean isSsl) {
      this.isSsl = isSsl;
   }

   public void setSecured(boolean secured) {
      this.secured = secured;
   }

   public boolean isHelp() {
      return help;
   }

   public void setHelp(boolean help) {
      this.help = help;
   }

   public int getReps() {
      return reps;
   }

   public void setReps(int reps) {
      if (reps < 1) {
         throw new IllegalArgumentException("Number of repetitions must be at least 1.");
      }
      this.reps = reps;
   }

   public long getInterval() {
      return interval;
   }

   public void setInterval(long interval) {
      this.interval = interval;
   }

   public String getPlainTextPassword() {
      return plainTextPassword;
   }

   public void setPlainTextPassword(String plainTextPassword) {
      this.plainTextPassword = plainTextPassword;
   }

   public boolean isArchivedLogs() {
      return archivedLogs;
   }

   public void setArchivedLogs(boolean archivedLogs) {
      this.archivedLogs = archivedLogs;
   }

   public boolean isAccessLogs() {
      return accessLogs;
   }

   public void setAccessLogs(boolean accessLogs) {
      this.accessLogs = accessLogs;
   }

   public boolean isBzip() {
      return isBzip;
   }

   public void setBzip(boolean bzip) {
      isBzip = bzip;
   }

   public String getUrl() {
      return getProtocol() + "://" + getHost() + ":" + getPort();
   }

   public String getProtocol(){
      if (this.isSsl) {
         return  "https";
      } else {
         return "http";
      }
   }

   @Override
   public String toString() {
      return "InputParams{" +
         "help=" + help +
         ", outputDir='" + outputDir + '\'' +
         ", host='" + host + '\'' +
         ", port=" + port +
         ", isSsl=" + isSsl +
         ", diagType='" + diagType + '\'' +
         ", reps=" + reps +
         ", interval=" + interval +
         ", isBzip=" + isBzip +
         ", archivedLogs=" + archivedLogs +
         ", scrubFiles=" + scrubFiles +
         ", skipVerification=" + skipVerification +
         ", keystore='" + keystore + '\'' +
         ", skipLogs=" + skipLogs +
         ", skipAccessLogs=" + accessLogs +
         ", secured=" + secured +
         ", wasPortSet=" + wasPortSet +
         '}';
   }
}
