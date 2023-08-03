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
import java.util.HashMap;
import java.util.Map;

/**
 * Generate a manifest containing the basic runtime info for the diagnostic
 * runtime.
 */
public class GenerateManifest implements Command {
   private final Logger logger = LogManager.getLogger(GenerateManifest.class);

   public void execute(DiagnosticContext context) {
      logger.info(Constants.CONSOLE, "Writing legacy [manifest.json].");

      try {
         ObjectMapper mapper = new ObjectMapper();
         mapper.enable(SerializationFeature.INDENT_OUTPUT);

         Map<String, Object> manifest = new HashMap<>();

         manifest.put(Constants.DIAG_VERSION, context.diagVersion);
         manifest.put("Product Version", context.version);
         manifest.put("collectionDate", SystemProperties.getUtcDateTimeString());
         manifest.put("diagnosticInputs", context.diagnosticInputs.toString());
         manifest.put("runner", context.diagnosticInputs.runner);

         mapper.writeValue(new File(context.tempDir + SystemProperties.fileSeparator + "manifest.json"), manifest);
      } catch (Exception e) {
         logger.info(Constants.CONSOLE, "Error creating the manifest file", e);
      }
   }
}
