package com.elastic.support.diagnostics.chain;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.diagnostics.InputParams;
import com.elastic.support.util.RestExec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DiagnosticContext {

   Logger logger = LogManager.getLogger(DiagnosticContext.class);

   protected Map<String, Object> attributes = new LinkedHashMap<>();
   InputParams inputParams;
   RestExec restExec;
   Map config;
   List<String> messages = new ArrayList<String>();
   String version = "";
   String clusterName = "";
   String outputDir = "";
   String tempDir = "";
   String manifest = "";
   String nodeString = "";
   String hostNode = "";
   String diagName = Constants.ES_DIAG;
   String diagNode = "";
   String pid = "0";
   String logDir = "";
   String esHome = "";
   int currentRep;
   boolean localAddressLocated = true;


   public DiagnosticContext(InputParams inputs){
      this.inputParams = inputs;
   }

   public boolean isLocalAddressLocated() {
      return localAddressLocated;
   }

   public void setLocalAddressLocated(boolean localAddressLocated) {
      this.localAddressLocated = localAddressLocated;
   }

   public String getDiagName() {
      return diagName;
   }

   public void setDiagName(String diagName) {
      this.diagName = diagName;
   }

   public String getDiagNode() {
      return diagNode;
   }

   public void setDiagNode(String diagNode) {
      this.diagNode = diagNode;
   }

   private static final String verTwo = "2(\\.\\d+)+";

   public String getPid() {
      return pid;
   }

   public void setPid(String pid) {
      this.pid = pid;
   }

   public String getHostNode() {
      return hostNode;
   }

   public void setHostNode(String hostNode) {
      this.hostNode = hostNode;
   }

   public String getNodeString() {
      return nodeString;
   }

   public void setNodeString(String nodeString) {
      this.nodeString = nodeString;
   }

   public String getTempDir() {
      return tempDir;
   }

   public void setTempDir(String tempDir) {
      this.tempDir = tempDir;
   }

   public void addMessage(String msg) {
      messages.add(msg);
   }

   public String getOutputDir() {
      return outputDir;
   }

   public void setOutputDir(String outputDir) {
      this.outputDir = outputDir;
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

   public String getClusterName() {
      return clusterName;
   }

   public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
   }

   public Map getConfig() {
      return config;
   }

   public void setConfig(Map config) {
      this.config = config;
   }

   public String getManifest() {
      return manifest;
   }

   public void setManifest(String manifest) {
      this.manifest = manifest;
   }

   public boolean isVersionTwoOrGreater() {
      return Pattern.matches(verTwo, version);
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

   public String getEsHome() {
      return esHome;
   }

   public void setEsHome(String esHome) {
      this.esHome = esHome;
   }

   public Map<String, Object> getAttributes() {
       return attributes;
   }

   public void setAttributes(Map<String, Object> attributes) {
       this.attributes = attributes;
   }

   public Object getAttribute(String key){
       return attributes.get(key);
   }

   public String getStringAttribute(String key){
       String ret = getTypedAttribute(key, String.class);
       return ret == null ? "" : ret;
   }

   public Long getLongAttribute(String key){
       Long ret = getTypedAttribute(key, Long.class);
       return ret == null ? 0L: ret;
   }

   public Integer getIntegerAttribute(String key){
       Integer ret = getTypedAttribute(key, Integer.class);
       return ret == null ? 0 : ret;
   }

   public Map<String, Object> getMappedAttribute(String key){
       Map<String, Object> ret = getTypedAttribute(key, Map.class);;
       Object obj = attributes.get(key);
       return ret == null ? new LinkedHashMap<String, Object>() : ret;
   }

   public List<Object> getListAttribute(String key){
       List<Object> ret = getTypedAttribute(key, List.class);;
       Object obj = attributes.get(key);
       return ret == null ? new ArrayList<Object>() : ret;
   }

   public boolean setAttribute(String key, Object value){
       // Send back true if we replaced an existing attribute and false if it was new
       Object ret = attributes.get(key);
       attributes.put(key, value);
       return ret == null ? false : true;
   }

   public <T> T getTypedAttribute(String key, Class<T> clazz){

       Object val = attributes.get(key);
       T castValue;

       if(val == null){
           logger.info("Attribute: " + key + "cannot be converted since it is null");
           return null;
       }

       try {
           castValue = clazz.cast(val);
       }
       catch (Exception e){
           logger.error("Attribute: " + key + " could not be cast to " + clazz, e);
           throw e;
       }

       return castValue;
   }
}
