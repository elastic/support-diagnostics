package com.elastic.support.diagnostics.commands;

import com.elastic.support.SystemProperties;
import com.elastic.support.diagnostics.DiagnosticContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class GenerateManifestCmd extends AbstractDiagnosticCmd {

    public boolean execute(DiagnosticContext context){

        String nodeString = context.getNodeString();
        if (nodeString == null || "".equals(nodeString)){
            context.addMessage("Could not create manifest - no node string to generate with.");
            return true;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(nodeString);
            JsonNode nodes = root.path("nodes");
            String clusterName = context.getClusterName();

            Iterator<JsonNode> it = nodes.iterator();

            Map<String, Object> cluster = new HashMap<>();
            cluster.put("diagToolVersion", getToolVersion());
            cluster.put("clusterName", clusterName);
            cluster.put("collectionDate", SystemProperties.getUtcDateString());

            File manifest = new File(context.getTempDir() + SystemProperties.fileSeparator + clusterName + "-manifest.json");
            mapper.writeValue(manifest, cluster);
            String manifestString = mapper.writeValueAsString(cluster);
            context.setManifest(manifestString);
            context.setNodeString("");
        } catch (Exception e) {
            logger.error("Error creating the manifest file\n", e);
            context.addMessage("Could not generate the manifest. Some output will not be available.");
        }

        return true;
    }

    public String getToolVersion(){
        String ver = GenerateManifestCmd.class.getPackage().getImplementationVersion() ;
        return (ver != null) ? ver : "Debug";
    }

   public String getIsoDate(){
      TimeZone tz = TimeZone.getTimeZone("UTC");
      DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
      df.setTimeZone(tz);
      String nowAsISO = df.format(new Date());
      return  nowAsISO;
   }
}
