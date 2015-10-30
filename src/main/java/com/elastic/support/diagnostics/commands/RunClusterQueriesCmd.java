package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.InputParams;
import com.elastic.support.SystemProperties;
import com.elastic.support.diagnostics.DiagnosticContext;
import com.elastic.support.diagnostics.RestModule;
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


        List textFileExtensions = (List) config.get("textFileExtensions");
        Map<String, String> statements = (Map<String, String>) config.get("restQueries");
        Set<Map.Entry<String, String>> entries = statements.entrySet();

        logger.debug("Generating full diagnostic.");

        for (Map.Entry<String, String> entry : entries) {
            try {
                queryName = entry.getKey();
                String query = entry.getValue();
                logger.debug(": now processing " + queryName + ", " + query);
                String url = inputs.getUrl() + "/" + query;
                String result = restModule.submitRequest(url);

                String ext;
                if (textFileExtensions.contains(queryName)) {
                    ext = ".txt";
                } else {
                    ext = ".json";
                }

                fileName = context.getTempDir() + SystemProperties.fileSeparator + queryName + ext;

                Files.write(Paths.get(fileName), result.getBytes());

                logger.debug("Done writing:" + fileName);
                System.out.println("Statistic " + queryName + " was retrieved and saved to disk.");

                //If it's nodes then we stash it so we can create the manifest
                if (queryName.equalsIgnoreCase("nodes")) {
                    context.setNodeString(result);
                }

            } catch (IOException ioe) {
                // If something goes wrong write the detail stuff to the log and then rethrow a RuntimeException
                // that will be caught at the top level and will contain a more generic user message
                logger.error("Diagnostic for:" + queryName + "couldn't be written", ioe);
                context.addMessage("Error writing file for statistic:" + queryName + ". There may be issues with the file system.  You may need to check for permissions or space issues.");
            } catch (Exception e) {
                // If they aren't Shield users this will generate an Exception so if it fails just continue and don't rethrow an Exception
                if (!"licenses".equalsIgnoreCase(queryName)) {
                    logger.error("Error retrieving the following diagnostic:  " + queryName + " - this stat will not be included.", e);
                    context.addMessage("Error retrieving the following diagnostic:  " + queryName + " - this stat will not be included.");
                }
            }
        }

        return true;
    }


}
