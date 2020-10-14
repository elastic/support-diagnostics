package com.elastic.support.diagnostics.chain;

import com.elastic.support.diagnostics.DiagnosticConfig;
import com.elastic.support.diagnostics.DiagnosticInputs;
import com.elastic.support.diagnostics.ProcessProfile;
import com.elastic.support.rest.RestEntry;
import com.vdurmont.semver4j.Semver;

import java.util.Map;

public class DiagnosticContext {

   public boolean runSystemCalls = true;
   public boolean isAuthorized = true;
   public boolean dockerPresent = false;

   public String clusterName = "";
   public String diagVersion;

   //public RestClient esRestClient;
   public DiagnosticConfig diagsConfig;
   public DiagnosticInputs diagnosticInputs;
   public ProcessProfile targetNode;
   public Semver version;

   public Map<String, RestEntry> elasticRestCalls;




}
