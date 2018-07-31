package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.JsonYamlUtils;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RunCommercialQueriesCmd extends AbstractQueryCmd {


   public boolean execute(DiagnosticContext context) {

      if(context.getInputParams().isHotThreads()){
         return true;
      }

      String runStatements = "";
      boolean runCommercial = false;
      Map<String, String> statements = null;
      String temp = context.getTempDir();

      try {
         JsonNode node = JsonYamlUtils.createJsonNodeFromFileName(temp, Constants.PLUGINS);
         List<String> vals = node.findValuesAsText("component");
         int majorVersion = Integer.parseInt(context.getVersion().split("\\.")[0]);
         int minorVersion = Integer.parseInt(context.getVersion().split("\\.")[1]);
         if( (majorVersion > 2 && majorVersion < 6) || (majorVersion == 6 && minorVersion < 3) ){
            for(String val: vals){
               if(val.contains("x-pack")){
                  runCommercial = true;
                  break;
               }
            }
         }
         else if ( majorVersion == 6 && minorVersion >= 3){
            runCommercial = true;
         }
         else if (majorVersion <= 2){
            if(vals.contains("shield")){
               runCommercial = true;
            }
         }
         else{
            System.out.println("Unrecognized major version");
            return false;
         }

         if(! runCommercial){
            logger.info("No commercial plugins found - bypassing those REST calls.");
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
         logger.info("Error querying commerical functionality.");
         logger.error(e);
      }

      return true;
   }

}
