package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.DiagnosticInputs;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.rest.RestExec;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseQueryCmd implements Command {

    protected final Logger logger = LogManager.getLogger(BaseQueryCmd.class);

    public void runQuery(String host, int port, String scheme, String queryName, String query, String tempDir) {

        RestExec restExec = GlobalContext.getRestExec();
        DiagnosticInputs diagnosticInputs = GlobalContext.getDiagnosticInputs();


        logger.debug(": now processing " + queryName + ", " + query);

        logger.info("Currently running query: {}", query);

        String fileName = buildFileName(queryName, context);


      );

        restExec.execConfiguredDiagnosticQuery(query, fileName, host, port, scheme);
    }

    protected void runQueries(Set<Map.Entry<String, String>> entries, String host, int port, String scheme, String queryName){

        String tempDir = context.getTempDir();

        for (Map.Entry<String, String> entry : entries) {
            String queryName = entry.getKey();
            String query = entry.getValue();
            runQuery(queryName, query, tempDir);
        }

    }

    protected String buildFileName(String queryName) {

        List textFileExtensions = (List) GlobalContext
                .getConfig().get("textFileExtensions");

        String ext;
        if (textFileExtensions.contains(queryName)) {
            ext = ".txt";
        } else {
            ext = ".json";
        }
        String fileName = GlobalContext.getDiagnosticInputs().getTempDir() + SystemProperties.fileSeparator + queryName + ext;

        return fileName;


    }

    private boolean isRetryable(int statusCode) {

        if (statusCode == 400) {
            logger.info("No data retrieved.");
            return true;
        } else if (statusCode == 401) {
            logger.info("Authentication failure: invalid login credentials. Check logs for details.");
            return false;
        } else if (statusCode == 403) {
            logger.info("Authorization failure or invalid license. Check logs for details.");
            return false;
        } else if (statusCode == 404) {
            logger.info("Endpoint does not exist.");
            return true;
        } else if (statusCode > 500 && statusCode < 600) {
            logger.info("Unrecoverable server error.");
            return true;
        }

        return false;

    }


}