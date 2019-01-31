package com.elastic.support.diagnostics;

import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.StringUtils;

public class DiagnosticInputs extends BaseInputs {

   @Parameter(names = {"-?", "--help"}, description = "Help contents.", help = true)
   private boolean help;

   public boolean isHelp() {
      return help;
   }

   public void setHelp(boolean help) {
      this.help = help;
   }

   @Parameter(names = {"-o", "--out", "--output", "--outputDir"}, description = "Fully qualified path to output directory or c for current working directory.")
   private String outputDir;

   public String getOutputDir() {
      return outputDir;
   }

   public void setOutputDir(String outputDir) {
      this.outputDir = outputDir;
   }

   @Parameter(names = {"-h", "--host"}, description = "Required field.  Hostname, IP Address, or localhost.  HTTP access must be enabled.")
   private String host = "";

   public String getHost() {
      return host;
   }

   public void setHost(String host) {
      this.host = host;
   }

   @Parameter(names = {"--port"}, description = "HTTP or HTTPS listening port. Defaults to 9200.")
   private int port = 9200;

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

   @Parameter(names = {"-u", "--user"}, description = "User")
   private String user;

   public String getUser() {
      return user;
   }

   public void setUser(String username) {
      this.user = user;
   }

   @Parameter(names = {"-p", "--password"}, description = "Password", password = true)
   private String password;

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   @Parameter(names = {"-s", "--ssl"}, description = "Use SSL?  No value required, only the option.")
   private boolean isSsl = false;

   public boolean isSsl() {
      return isSsl;
   }

   public void setIsSsl(boolean isSsl) {
      this.isSsl = isSsl;
   }

   @Parameter(names = {"--type"}, description ="Diagnostic type to run. Enter standard, remote, logstash. Default is standard. Using remote will suppress retrieval of logs, configuration and system command info.")
   private String diagType = "standard";

   public String getDiagType() {
      return diagType;
   }

   public void setDiagType(String diagType) {
      this.diagType = diagType;
   }

   @Parameter(names = {"--ptp"}, description = "Insecure plain text password - allows you to input the password as a plain text argument for scripts. WARNING: Exposes passwords in clear text. Inherently insecure.")
   private String plainTextPassword = "";

   public String getPlainTextPassword() {
      return plainTextPassword;
   }

   public void setPlainTextPassword(String plainTextPassword) {
      this.plainTextPassword = plainTextPassword;
   }

   @Parameter(names = {"--reps"}, description = "Number of times to execute the diagnostic. Use to create multiple runs at timed intervals.")
   private int reps = 1;

   public int getReps() {
      return reps;
   }

   public void setReps(int reps) {
      this.reps = reps;
   }

   @Parameter(names = {"--interval"}, description = "Timed interval at which to execute the diagnostic. Minimum interval is 10 minutes.")
   private int interval = 600000;

   public int getInterval() {
      return interval;
   }

   public void setInterval(int interval) {
      this.interval = interval;
   }

   @Parameter(names = {"--scrub"}, description = "Set to true to use the scrub.yml dictionary to scrub logs and config files.  See README for more info.")
   private boolean scrubFiles = false;

   public boolean isScrubFiles() {
      return scrubFiles;
   }

   public void setScrubFiles(boolean scrubFiles){
      this.scrubFiles = scrubFiles;
   }

   @Parameter(names = {"--noVerify"}, description = "Use this option to bypass hostname verification for certificate. This is inherently unsafe and NOT recommended.")
   private boolean skipVerification = false;

   public boolean isSkipVerification() {
      return skipVerification;
   }

   public void setSkipVerification(boolean skipVerification) {
      this.skipVerification = skipVerification;
   }

   @Parameter(names= {"--keystore"}, description = "Keystore for client certificate.")
   private String keystore;

   public String getKeystore() {
      return keystore;
   }

   public void setKeystore(String keystore) {
      this.keystore = keystore;
   }

   @Parameter(names= {"--keystorePass"}, description = "Keystore password for client certificate.", password = true)
   private String keystorePass;

   public String getKeystorePass() {
      return keystorePass;
   }

   public void setKeystorePass(String keystorePass) {
      this.keystorePass = keystorePass;
   }

   @Parameter(names = {"--accessLogs"}, description = "Use this option to collect access logs as well.")
   private boolean accessLogs = false;

   public boolean isAccessLogs() {
      return accessLogs;
   }

   public void setAccessLogs(boolean accessLogs) {
      this.accessLogs = accessLogs;
   }

   @Parameter(names = {"--bypassDiagVerify"}, description = "Bypass the diagnostic version check.")
   private boolean bypassDiagVerify = false;

   public boolean isBypassDiagVerify() {
      return bypassDiagVerify;
   }

   public void setBypassDiagVerify(boolean bypassDiagVerify) {
      this.bypassDiagVerify = bypassDiagVerify;
   }

   private boolean secured = false;

   public boolean isSecured() {
      return (StringUtils.isNotEmpty(this.user) && StringUtils.isNotEmpty(this.password));
   }

   public String getScheme(){
      if(this.isSsl){
         return "http";
      }
      else{
         return "https";
      }
   }

   public boolean validate(){

      // If we're in help just shut down.
      if (isHelp()) {
         this.jCommander.usage();
         return false;
      }

      return (validateAuth() && validateIntervals());

   }

   public boolean validateAuth() {

      String ptPassword = this.getPlainTextPassword();
      if (StringUtils.isNotEmpty(ptPassword) ){
         this.password = ptPassword;
      }

      if( ! isAuthValid(user, password) ){
         logger.info("Input error: If authenticating both user and password are required.");
         this.jCommander.usage();
         return false;
      }

      return true;

   }

   private boolean isAuthValid(String user, String password){

      if( (StringUtils.isNotEmpty(user) && StringUtils.isEmpty(password)) ||
              (StringUtils.isNotEmpty(password) && StringUtils.isEmpty(user)) ){
         return false;
      }
      return true;

   }

   public boolean validateIntervals() {

      boolean repsOK = true, intervalOK = true;

      if (this.getReps() > 1) {

         if (this.getReps() > 6) {
            logger.info("Reps specified exceed the maximum allowed.");
            logger.info("Use --help for allowed values.");
            repsOK = false;
         }

         if (this.getInterval() < 10) {
            logger.info("Interval specificed is lower than the minium allowed.");
            logger.info("Use --help for allowed values.");
            intervalOK = false;
         }
      }

      return (repsOK && intervalOK);

   }

   @Override
   public String toString() {
      return "DiagnosticInputs{" +
         "help=" + help +
         ", outputDir='" + outputDir + '\'' +
         ", host='" + host + '\'' +
         ", port=" + port +
         ", isSsl=" + isSsl +
         ", diagType='" + diagType + '\'' +
         ", skipVerification=" + skipVerification +
         ", keystore='" + keystore + '\'' +
         ", skipAccessLogs=" + accessLogs +
         ", secured=" + secured +
         '}';
   }
}
