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
public class GenerateDiagnosticManifest implements Command {
   private final Logger logger = LogManager.getLogger(GenerateDiagnosticManifest.class);

   public void execute(DiagnosticContext context) {
      logger.info(Constants.CONSOLE, "Writing [diagnostic_manifest.json].");

      String product = "elasticsearch";

      if (context.diagnosticInputs.diagType.startsWith("kibana")) {
         product = "kibana";
      } else if (context.diagnosticInputs.diagType.startsWith("logstash")) {
         product = "logstash";
      }

      try {
         ObjectMapper mapper = new ObjectMapper();
         mapper.enable(SerializationFeature.INDENT_OUTPUT);

         Map<String, Object> manifest = new HashMap<>();

         manifest.put("diagnostic", context.diagVersion);
         manifest.put("type", product + "_diagnostic");
         manifest.put("product", product);
         // Logstash does not lookup a version currently
         manifest.put("version", context.version != null ? context.version.getVersion() : "0.0.0");
         manifest.put("timestamp", SystemProperties.getUtcDateTimeString());
         manifest.put("flags", context.diagnosticInputs.toString());
         manifest.put("runner", context.diagnosticInputs.runner);
         manifest.put("mode", context.diagnosticInputs.mode);

         mapper.writeValue(
               new File(context.tempDir + SystemProperties.fileSeparator + "diagnostic_manifest.json"),
               manifest);
      } catch (Exception e) {
         logger.info(Constants.CONSOLE, "Error creating [diagnostic_manifest.json]", e);
      }
   }
}
