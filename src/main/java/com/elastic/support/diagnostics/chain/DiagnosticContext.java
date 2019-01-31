package com.elastic.support.diagnostics.chain;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.diagnostics.InputParams;
import com.elastic.support.util.RestExec;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DiagnosticContext {

   InputParams inputParams;
   RestExec restExec;
   Map config;
   String version = "";
   String tempDir = "";
   String diagName = Constants.ES_DIAG;
   String pid = "0";
   String logDir = "";
   String archiveFileName;
   String systemDigest;
   String diagVersion;
   int currentRep;

   public String getDiagName() {
      return diagName;
   }

   public void setDiagName(String diagName) {
      this.diagName = diagName;
   }

   public String getPid() {
      return pid;
   }

   public void setPid(String pid) {
      this.pid = pid;
   }

   public String getTempDir() {
      return tempDir;
   }

   public void setTempDir(String tempDir) {
      this.tempDir = tempDir;
   }

   public InputParams getInputParams() {
      return inputParams;
   }

   public void setInputParams(InputParams inputParams) {
      this.inputParams = inputParams;
   }

   public RestExec getRestExec() {
      return restExec;
   }

   public void setRestExec(RestExec restExec) {
      this.restExec = restExec;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public Map getConfig() {
      return config;
   }

   public void setConfig(Map config) {
      this.config = config;
   }

   public int getCurrentRep() {
      return currentRep;
   }

   public void setCurrentRep(int currentRep) {
      this.currentRep = currentRep;
   }

   public void setLogDir(String logDir) {
      this.logDir = logDir;
   }

   public String getLogDir() {
      return logDir;
   }


   public String getArchiveFileName() {
      return archiveFileName;
   }

   public void setArchiveFileName(String archiveFileName) {
      this.archiveFileName = archiveFileName;
   }

   public String getSystemDigest() {
      return systemDigest;
   }

   public void setSystemDigest(String systemDigest) {
      this.systemDigest = systemDigest;
   }

   public String getDiagVersion() {
      return diagVersion;
   }

   public void setDiagVersion(String diagVersion) {
      this.diagVersion = diagVersion;
   }
}
