package com.elastic.support.diagnostics.chain;

import com.elastic.support.config.DiagConfig;
import com.elastic.support.config.DiagnosticInputs;
import com.elastic.support.rest.RestClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticContext {

   private String version = "";
   private String tempDir = "";
   private String pid = "0";
   private String logDir = "";
   private String systemDigest;
   private String diagVersion;
   private  JsonNode nodeManifest;
   private boolean isDocker = false;
   private List<String> dockerContainers = new ArrayList<>();

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

   public boolean isDocker() {
      return isDocker;
   }

   public void setDocker(boolean docker) {
      isDocker = docker;
   }

   public List<String> getDockerContainers() {
      return dockerContainers;
   }

   public void setDockerContainers(List<String> dockerContainers) {
      this.dockerContainers = dockerContainers;
   }
}
