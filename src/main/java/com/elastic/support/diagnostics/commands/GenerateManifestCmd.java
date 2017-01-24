package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.InputParams;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class GenerateManifestCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      logger.info("Writing diagnostic manifest.");
      try {
         ObjectMapper mapper = new ObjectMapper();
         mapper.enable(SerializationFeature.INDENT_OUTPUT);
         String clusterName = context.getClusterName();
         Map<String, Object> cluster = new HashMap<>();
         cluster.put("diagToolVersion", getToolVersion());
         cluster.put("clusterName", clusterName);
         cluster.put("collectionDate", SystemProperties.getUtcDateString());
         cluster.put("host", context.getAttribute("hostName"));
         cluster.put("diagNode", context.getHostNode());
         InputParams params = context.getInputParams();
         cluster.put("inputs", params.toString());

         File manifest = new File(context.getTempDir() + SystemProperties.fileSeparator + "manifest.json");
         mapper.writeValue(manifest, cluster);

      } catch (Exception e) {
         logger.error("Error creating the manifest file\n", e);
      }

      return true;
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
