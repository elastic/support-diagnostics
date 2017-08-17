package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.JsonYamlUtils;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RunCommercialQueriesCmd extends AbstractQueryCmd {

   private static final String PLUGINS = "plugins.json";


   public boolean execute(DiagnosticContext context) {
      String runStatements = "";
      boolean runCommercial = false;
      Map<String, String> statements = null;
      String temp = context.getTempDir();

      try {
         JsonNode node = JsonYamlUtils.createJsonNodeFromFileName(temp, PLUGINS);
         List<String> vals = node.findValuesAsText("component");
         int majorVersion = Integer.parseInt(context.getVersion().split("\\.")[0]);
         if(majorVersion > 2){
            if(vals.contains("x-pack")){
               context.setAttribute("commercialDir", "x-pack");
               runCommercial = true;

            }
         }
         else if (majorVersion <= 2){
            if(vals.contains("shield")){
               context.setAttribute("commercialDir", "shield");
               runCommercial = true;
            }
         }
         else{
            System.out.println("Unrecognized major version");
            return false;
         }

         if(! runCommercial){
            System.out.println("No commercial plugins found - bypassing those REST calls.");
            return true;
         }

         runStatements = "commercial-" + majorVersion;
         statements = (Map<String, String>) context.getConfig().get(runStatements);

         if (statements == null || statements.isEmpty()) {
            throw new IllegalArgumentException("Rest calls for commercial plugins, version:" + majorVersion + " were not found.");
         }

         Set<Map.Entry<String, String>> entries = statements.entrySet();

         logger.debug("Generating full diagnostic.");

         for (Map.Entry<String, String> entry : entries) {
            runQuery(entry, context);
         }
      } catch (Exception e) {
         System.out.println("Error querying commerical plugins");
         logger.error("Error querying commercial plugins.", e);
      }

      return true;
   }

}
