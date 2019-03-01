package com.elastic.support.diagnostics.chain;

import com.elastic.support.config.DiagConfig;
import com.elastic.support.config.DiagnosticInputs;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestExec;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public class DiagnosticContext {

   private String version = "";
   private String tempDir = "";
   private String pid = "0";
   private String logDir = "";
   private String systemDigest;
   private String diagVersion;
   private  JsonNode nodeManifest;

   private RestClient genericClient, esRestClient;
   private  DiagConfig diagsConfig;
   private  DiagnosticInputs diagnosticInputs;

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public String getTempDir() {
      return tempDir;
   }

   public void setTempDir(String tempDir) {
      this.tempDir = tempDir;
   }

   public String getPid() {
      return pid;
   }

   public void setPid(String pid) {
      this.pid = pid;
   }

   public String getLogDir() {
      return logDir;
   }

   public void setLogDir(String logDir) {
      this.logDir = logDir;
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

   public JsonNode getNodeManifest() {
      return nodeManifest;
   }

   public void setNodeManifest(JsonNode nodeManifest) {
      this.nodeManifest = nodeManifest;
   }

   public DiagConfig getDiagsConfig() {
      return diagsConfig;
   }

   public void setDiagsConfig(DiagConfig diagsConfig) {
      this.diagsConfig = diagsConfig;
   }

   public DiagnosticInputs getDiagnosticInputs() {
      return diagnosticInputs;
   }

   public void setDiagnosticInputs(DiagnosticInputs diagnosticInputs) {
      this.diagnosticInputs = diagnosticInputs;
   }

   public RestClient getGenericClient() {
      return genericClient;
   }

   public void setGenericClient(RestClient genericClient) {
      this.genericClient = genericClient;
   }

   public RestClient getEsRestClient() {
      return esRestClient;
   }

   public void setEsRestClient(RestClient esRestClient) {
      this.esRestClient = esRestClient;
   }
}
