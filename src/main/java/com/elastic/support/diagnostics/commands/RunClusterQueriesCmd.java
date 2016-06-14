package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.InputParams;
import com.elastic.support.SystemProperties;
import com.elastic.support.diagnostics.DiagnosticContext;
import com.elastic.support.diagnostics.RestModule;
import java.io.InputStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RunClusterQueriesCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      InputParams inputs = context.getInputParams();
      Map config = context.getConfig();
      RestModule restModule = context.getRestModule();
      String queryName = null;
      String fileName = null;

      String majorVersion = context.getVersion().substring(0, 1);

      List textFileExtensions = (List) config.get("textFileExtensions");
      Map<String, String> statements = (Map<String, String>) config.get("restQueries-" + majorVersion);
      Set<Map.Entry<String, String>> entries = statements.entrySet();

      logger.debug("Generating full diagnostic.");

      for (Map.Entry<String, String> entry : entries) {
            queryName = entry.getKey();
            String query = entry.getValue();
            logger.debug(": now processing " + queryName + ", " + query);
            String url = inputs.getUrl() + "/" + query;
            logger.info("Currently running the following query:" + queryName);

            String ext;
            if (textFileExtensions.contains(queryName)) {
               ext = ".txt";
            } else {
               ext = ".json";
            }

            fileName = context.getTempDir() + SystemProperties.fileSeparator + queryName + ext;
            restModule.submitRequest(url, queryName, fileName);
      }

      return true;
   }


}
