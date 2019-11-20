package com.elastic.support.diagnostics.chain;

import com.elastic.support.diagnostics.DiagConfig;
import com.elastic.support.diagnostics.DiagnosticInputs;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.vdurmont.semver4j.Semver;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;

public class DiagnosticContext {

   private Semver version;
   private String tempDir = "";
   private String pid = "0";
   private String logDir = "";
   private String home = "";
   private String systemDigest;
   private String diagVersion;
   private  JsonNode nodeManifest;
   private boolean isDocker = false;
   private boolean bypassSystemCalls = false;
   private boolean bypassLogs = false;

   private List<String> dockerContainers = new ArrayList<>();

   private RestClient genericClient, esRestClient;
   private List<RestEntry> elasticRestCalls, logstashRestCalls;
   private  DiagConfig diagsConfig;
   private  DiagnosticInputs diagnosticInputs;
   private boolean isAuthorized;

   public Semver getVersion() {
      return version;
   }

   public void setVersion(Semver version) {
      this.version = version;
   }

   public List<RestEntry> getElasticRestCalls() {
      return elasticRestCalls;
   }

   public void setElasticRestCalls(List<RestEntry> elasticRestCalls) {
      this.elasticRestCalls = elasticRestCalls;
   }

   public List<RestEntry> getLogstashRestCalls() {
      return logstashRestCalls;
   }

   public void setLogstashRestCalls(List<RestEntry> logstashRestCalls) {
      this.logstashRestCalls = logstashRestCalls;
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

   public String getHome() {
      return home;
   }

   public void setHome(String home) {
      this.home = home;
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

   public boolean isAuthorized() {
      return isAuthorized;
   }

   public void setAuthorized(boolean isAuthorized) {
      this.isAuthorized = isAuthorized;
   }

   public boolean isBypassSystemCalls() {
      return bypassSystemCalls;
   }

   public void setBypassSystemCalls(boolean bypassSystemCalls) {
      this.bypassSystemCalls = bypassSystemCalls;
   }

   public boolean isBypassLogs() {
      return bypassLogs;
   }

   public void setBypassLogs(boolean bypassLogs) {
      this.bypassLogs = bypassLogs;
   }
}
