package com.elastic.support.diagnostics.chain;

import com.elastic.support.config.DiagnosticInputs;
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
   private RestExec restExec;
   private Map<String, List<String>> chains;
   private Map diagsConfig;
   private  JsonNode nodeManifest;
   private  DiagnosticInputs diagnosticInputs;

}
