package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.Diagnostic;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.diagnostics.chain.GlobalContext;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class RunClusterQueriesCmd extends BaseQueryCmd {

   public void execute(DiagnosticContext context) {
      Map config = (Map) GlobalContext.getConfig().get("rest-calls");
      String version = context.getVersion();

      Map<String, String> statements = buildStatementsByVersion(version, config);

      Set<Map.Entry<String, String>> entries = statements.entrySet();

      logger.debug("Generating full diagnostic.");

      for (Map.Entry<String, String> entry : entries) {
         runQuery(entry, context);
      }

   }

   public Map<String, String> buildStatementsByVersion(String version, Map calls ) {

      String[] ver = version.split("\\.");
      int major = Integer.parseInt(ver[0]);
      int minor = Integer.parseInt(ver[1]);

      Map statements = new LinkedHashMap<>();

      // First get the statements that work for all versions
      statements.putAll((Map)calls.get("common"));

      // Go through any additional calls up to the current version
      Map versionSpecificCalls = ((Map) calls.get("versions"));
      for (int ma = 1; ma <= major; ma++) {
         String majKey = "major-" + ma ;

         // See if there are calls specific to this major version
         // If nothing skip to next major version
         Map majorVersionCalls = getCalls(majKey, versionSpecificCalls);
         if(majorVersionCalls.size() == 0){
            continue;
         }

         //If we are on a lower major version get all the minors
         if( ma < major){
            Collection values = majorVersionCalls.values();
            for(Object verEntry : values ){
               statements.putAll((Map)verEntry);
            }
         }
         // Otherwise just get the ones at or below the input minor
         else{
            for (int mi = 0; mi <= minor; mi++) {
               String minKey = "minor-" + mi ;
               statements.putAll(getCalls(minKey, majorVersionCalls));
            }
         }
      }

      return statements;
   }

   // Don't want to check each potential addiition to the base statements for emptiness so just
   // use an emoty map if there are no results for this version check.
   public Map getCalls(String key, Map calls){
      Map result = (Map) calls.get(key);
      if (result == null){
         return new LinkedHashMap();
      }
      return result;
   }

}
