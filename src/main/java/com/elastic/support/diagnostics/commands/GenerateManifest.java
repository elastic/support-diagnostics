/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagnosticInputs;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class GenerateManifest implements Command {

   /**
    * Generate a manifest containing the basic runtime info for the diagnostic runtime.
    * Some of the values we get, like the Diagnostic version will be used again
    * downstream.
    */
   private final Logger logger = LogManager.getLogger(GenerateManifest.class);

   public void execute(DiagnosticContext context) {

      // Dump out background information to provide information for potentially debugging the diagnostic if an issue occurs.
      logger.info(Constants.CONSOLE, "Writing diagnostic manifest.");
      try {
         ObjectMapper mapper = new ObjectMapper();
         mapper.enable(SerializationFeature.INDENT_OUTPUT);

         Map<String, Object> manifest = new HashMap<>();

         String diagVersion = context.diagVersion;
         manifest.put(Constants.DIAG_VERSION, diagVersion);
         manifest.put("Product Version", context.version);
         manifest.put("collectionDate", SystemProperties.getUtcDateTimeString());
         manifest.put("diagnosticInputs", context.diagnosticInputs.toString());

         File manifestFile = new File(context.tempDir + SystemProperties.fileSeparator + "manifest.json");
         mapper.writeValue(manifestFile, manifest);
      } catch (Exception e) {
         logger.info(Constants.CONSOLE, "Error creating the manifest file", e);
      }
   }

   public String getIsoDate() {
      TimeZone tz = TimeZone.getTimeZone("UTC");
      DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
      df.setTimeZone(tz);
      String nowAsISO = df.format(new Date());
      return nowAsISO;
   }
}
