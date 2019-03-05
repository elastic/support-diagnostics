package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.Constants;
import com.elastic.support.config.DiagnosticInputs;
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

public class GenerateManifestCmd  implements Command {

   private final Logger logger = LogManager.getLogger(GenerateManifestCmd.class);

   public void execute(DiagnosticContext context) {

      // Dump out background information to provide information for potentially debugging the diagnostic if an issue occurs.
      logger.info("Writing diagnostic manifest.");
      try {
         ObjectMapper mapper = new ObjectMapper();
         mapper.enable(SerializationFeature.INDENT_OUTPUT);

         Map<String, Object> manifest = new HashMap<>();

         String diagVersion = getToolVersion();
         manifest.put(Constants.DIAG_VERSION, diagVersion);

         context.setDiagVersion(diagVersion);
         manifest.put("collectionDate", SystemProperties.getUtcDateString());

         DiagnosticInputs params = context.getDiagnosticInputs();
         manifest.put("diagnosticInputs", params.toString());

         File manifestFile = new File(context.getTempDir() + SystemProperties.fileSeparator + "manifest.json");
         mapper.writeValue(manifestFile, manifest);
      } catch (Exception e) {
         logger.error("Error creating the manifest file", e);
      }
   }

   public String getToolVersion() {
      String ver = GenerateManifestCmd.class.getPackage().getImplementationVersion();
      return (ver != null) ? ver : "Debug";
   }

   public String getIsoDate() {
      TimeZone tz = TimeZone.getTimeZone("UTC");
      DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
      df.setTimeZone(tz);
      String nowAsISO = df.format(new Date());
      return nowAsISO;
   }
}
