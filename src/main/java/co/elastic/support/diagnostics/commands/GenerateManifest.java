/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.Constants;
import co.elastic.support.diagnostics.chain.Command;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.util.SystemProperties;
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

         manifest.put(Constants.DIAG_VERSION, context.diagVersion);
         manifest.put("Product Version", context.version);
         manifest.put("collectionDate", SystemProperties.getUtcDateTimeString());
         manifest.put("diagnosticInputs", context.diagnosticInputs.toString());
         manifest.put("runner", context.diagnosticInputs.runner);

         File manifestFile = new File(context.tempDir + SystemProperties.fileSeparator + "manifest.json");
         mapper.writeValue(manifestFile, manifest);
      } catch (Exception e) {
         logger.info(Constants.CONSOLE, "Error creating the manifest file", e);
      }
   }
}
