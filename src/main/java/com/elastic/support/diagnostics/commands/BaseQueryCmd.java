package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.Diagnostic;
import com.elastic.support.diagnostics.DiagnosticInputs;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestExec;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public abstract class AbstractQueryCmd  implements Command {

   protected final Logger logger = LogManager.getLogger(AbstractQueryCmd.class);

   public void runQuery(Map.Entry<String, String> entry, DiagnosticContext context) {

      RestExec restExec = Diagnostic.globalContext.getRestExec();
      DiagnosticInputs diagnosticInputs = Diagnostic.globalContext.getDiagnosticInputs();
      String queryName = entry.getKey();
      String query = entry.getValue();

      logger.debug(": now processing " + queryName + ", " + query);

      String url = diagnosticInputs.getUrl() + "/" + query;
      logger.info("Currently running query: {}", query);

      String fileName = buildFileName(queryName, context );
      restExec.execConfiguredQuery(url, fileName);
   }

   protected String buildFileName(String queryName, DiagnosticContext context){

      List textFileExtensions = (List) Diagnostic.globalContext.getConfig().get("textFileExtensions");

      String ext;
      if (textFileExtensions.contains(queryName)) {
         ext = ".txt";
      } else {
         ext = ".json";
      }
      String fileName = context.getTempDir() + SystemProperties.fileSeparator + queryName + ext;

      return fileName;


   }


}