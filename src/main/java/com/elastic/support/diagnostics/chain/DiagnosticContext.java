package com.elastic.support.diagnostics.chain;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.diagnostics.DiagnosticInputs;
import com.elastic.support.rest.RestExec;

import java.util.Map;

public class DiagnosticContext {

   String version = "";
   String tempDir = "";
   String pid = "0";
   String logDir = "";
   String systemDigest;
   String diagVersion;

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

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public void setLogDir(String logDir) {
      this.logDir = logDir;
   }

   public String getLogDir() {
      return logDir;
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
