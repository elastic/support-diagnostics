/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.diagnostics.chain;

import co.elastic.support.diagnostics.DiagnosticInputs;
import co.elastic.support.diagnostics.ProcessProfile;
import co.elastic.support.rest.RestEntry;
import co.elastic.support.diagnostics.DiagConfig;
import com.vdurmont.semver4j.Semver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DiagnosticContext {

   public boolean runSystemCalls = true;
   public boolean isAuthorized = true;
   public boolean dockerPresent = false;
   public int perPage = 0;

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
