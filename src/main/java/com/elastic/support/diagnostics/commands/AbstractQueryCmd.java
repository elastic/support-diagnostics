package com.elastic.support.diagnostics.commands;

import com.elastic.support.SystemProperties;
import com.elastic.support.diagnostics.DiagnosticContext;
import com.elastic.support.diagnostics.InputParams;
import com.elastic.support.diagnostics.RestModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public abstract class AbstractQueryCmd extends AbstractDiagnosticCmd {

   public void runQuery(Map.Entry<String, String> entry, DiagnosticContext context){

      RestModule restModule = context.getRestModule();
      InputParams inputs = context.getInputParams();
      List textFileExtensions = (List) context.getConfig().get("textFileExtensions");
      String queryName = entry.getKey();
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

      String fileName = context.getTempDir() + SystemProperties.fileSeparator + queryName + ext;
      restModule.submitRequest(inputs.getProtocol(), inputs.getHost(), inputs.getPort(), query, queryName, fileName);
   }

}