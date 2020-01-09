package com.elastic.support.diagnostics.chain;

import com.elastic.support.diagnostics.DiagConfig;
import com.elastic.support.diagnostics.DiagnosticInputs;
import com.elastic.support.diagnostics.ProcessProfile;
import com.elastic.support.rest.RestEntry;
import com.vdurmont.semver4j.Semver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DiagnosticContext {

   public boolean runSystemCalls = true;
   public boolean isAuthorized = true;
   public boolean dockerPresent = false;

   public String clusterName = "";
   public String tempDir = "";
   public String diagVersion;

   //public RestClient esRestClient;
   public DiagConfig diagsConfig;
   public DiagnosticInputs diagnosticInputs;
   public ProcessProfile targetNode;
   public Semver version;

   public List<String> dockerContainers = new ArrayList<String>();
   public Map<String, RestEntry> elasticRestCalls;




}
