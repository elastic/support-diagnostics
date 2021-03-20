package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.diagnostics.JavaPlatform;
import com.elastic.support.diagnostics.ProcessProfile;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestEntry;
import com.elastic.support.rest.RestResult;
import com.elastic.support.rest.RestEntryConfig;
import com.elastic.support.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import java.io.FileWriter;
import java.io.IOException;

/**
 * RunKibanaQueries executes the version-dependent REST API calls against Kibana.
 */
public class RunKibanaQueries extends BaseQuery {

    private static final Logger logger = LogManager.getLogger(RunKibanaQueries.class);

    /**
    * Create a new ProcessProfile object and extract the information from fileName to get PID and OS.
    *
    * @param  String tempDir
    * @param  String fileName
    * @param  DiagnosticContext context
    * @return         ProcessProfile object
    */
    private ProcessProfile getProfile(String tempDir, String fileName, DiagnosticContext context) {
        ProcessProfile profile = new ProcessProfile();
        context.targetNode = profile;
        JsonNode nodeData = JsonYamlUtils.createJsonNodeFromFileName(tempDir, fileName);
        profile.pid = nodeData.path("process").path("pid").asText();
        profile.os = SystemUtils.parseOperatingSystemName(nodeData.path("os").path("platform").asText());
        profile.javaPlatform = getJavaPlatformOs(profile.os);

        return profile;
    }

    /**
    * this private function will create a new JavaPlatform object
    *
    * @param  String profileOs
    * @return         JavaPlatform object
    */
    private JavaPlatform getJavaPlatformOs(String profileOs) {
        JavaPlatform javaPlatformOs = new JavaPlatform(profileOs);
        return javaPlatformOs;
    }

    /**
    * this private function will create new object RemoteSystem
    *
    * @param  String targetOS
    * @param  DiagnosticContext context
    * @return         RemoteSystem object
    */
    private RemoteSystem getRemoteSystem(String targetOS, DiagnosticContext context) {
        RemoteSystem syscmd = new RemoteSystem(
                            targetOS,
                            context.diagnosticInputs.remoteUser,
                            context.diagnosticInputs.remotePassword,
                            context.diagnosticInputs.host,
                            context.diagnosticInputs.remotePort,
                            context.diagnosticInputs.keyfile,
                            context.diagnosticInputs.pkiKeystorePass,
                            context.diagnosticInputs.knownHostsFile,
                            context.diagnosticInputs.trustRemote,
                            context.diagnosticInputs.isSudo
                    );
        ResourceCache.addSystemCommand(Constants.systemCommands, syscmd);
        return syscmd;
    }

    /**
    * On this function we will get create an LocalSystem object
    * depending the parseOperatingSystem value it will override or not the value of osName with the constant SystemProperties.osName
    *
    * @param  String osName
    * @param  Boolean parseOperatingSystem
    * @return         LocalSystem object
    */
    private LocalSystem getLocalSystem(String osName, Boolean parseOperatingSystem) {
        if (parseOperatingSystem == true) {
            osName = SystemUtils.parseOperatingSystemName(SystemProperties.osName);
        }
        LocalSystem syscmd = new LocalSystem(osName);
        ResourceCache.addSystemCommand(Constants.systemCommands, syscmd);
        return syscmd;
    }


    /**
    * CheckKibanaVersion (executed before) defined/set the context.elasticRestCalls.
    * here we will loop and create a List of RestEntry with the queries/APIs that need to be executed
    * You have two APIs that work differently and and may have or not many pages, so we use the getAllPages function
    * runQueries called in this function create a json file for each RestEntry set on the 'queries' variable.
    * Public function so we can test it.
    *
    * @param  RestClient client
    * @param  DiagnosticContext context
    * @return int
    */
    public int runBasicQueries(RestClient client, DiagnosticContext context) {

        int totalRetries = 0;
        List<RestEntry> queries = new ArrayList<>();
        
        for (Map.Entry<String, RestEntry> entry : context.elasticRestCalls.entrySet()) {

            String actionName = entry.getValue().getName().toString();
            if (actionName.equals("kibana_alerts") || actionName.equals("kibana_detection_engine_find")) {
                getAllPages(client, queries, context.perPage, entry.getValue());
            } else {
                queries.add(entry.getValue());
            }
        }
        totalRetries = runQueries(client, queries, context.tempDir, 0, 0);

        return totalRetries;
    }


    /**
    * On this function we will use the RestEntry action object to get the URL and execute the API one first time
    * to get the total of events defined in Kibana for that API.
    * Once we have the total of event we can calculate the number of pages/call we will need to execute
    * then we call getNewEntryPage to create a new RestEntry for each page.
    *
    * @param  RestClient client
    * @param  List<RestEntry> queries
    * @param  double perPage - This is the number of docusment we reques to the API (set to 100 in execute func / or n in unit test)
    * @param  RestEntry action
    */
    public void getAllPages(RestClient client, List<RestEntry> queries, double perPage, RestEntry action) {
        // get the values needed to the pagination.
        RestResult res = client.execQuery(String.format("%s?per_page=1", action.getUrl()));
        if (! res.isValid()) {
            throw new DiagnosticException( res.formatStatusMessage( "Could not retrieve Kibana API pagination - unable to continue."));
        }
        String result   = res.toString();
        JsonNode root   = JsonYamlUtils.createJsonNodeFromString(result);
        int total       = root.path("total").intValue();
        if (total > 0 && perPage > 0) {
            // Get the first actions page
            queries.add(getNewEntryPage(perPage, 1, action));
            // If there is more pages add the new queries
            if (perPage < total) {
                int numberPages = (int)Math.ceil(total / perPage);
                for (int i = 2; i <= numberPages; i++) {
                    queries.add(getNewEntryPage(perPage, i, action));
                }
            }
        }
    }

    /**
    * Get a new `RestEntry` with the proper querystring parameters for paging.
    *
    * @param  int perPage
    * @param  int page
    * @param  RestEntry action
    * @return RestEntry
    */
    private RestEntry getNewEntryPage(int perPage, int page, RestEntry action) {
        return new RestEntry(String.format("%s_%s", action.getName(), page), "", ".json", false, String.format("%s?per_page=%s&page=%s", action.getUrl(), perPage, page), false);
    }


    /**
    * This function is executed **after** runBasicQueries
    * Extract the information on the kibana_stats.json with getProfile function.
    * Within the function getRemoteSystem or getLocalSystem we set ResourceCache.addSystemCommand
    *
    * @param  DiagnosticContext context
    * @return SystemCommand
    */
    public SystemCommand execSystemCommands(DiagnosticContext context) {

        ProcessProfile profile = getProfile(context.tempDir, "kibana_stats.json", context);

        if (StringUtils.isEmpty(profile.pid) || profile.pid.equals("1")) {
            context.dockerPresent = true;
            context.runSystemCalls = false;
        }
        // Create and cache the system command type we need, local or remote...
        SystemCommand syscmd = null;
        switch (context.diagnosticInputs.diagType) {
            case Constants.kibanaRemote:
                String targetOS;
                if(context.dockerPresent){
                    targetOS = Constants.linuxPlatform;
                }
                else{
                    targetOS = profile.os;
                }
                
                syscmd = getRemoteSystem(targetOS, context);
                break;

            case Constants.kibanaLocal:
                if (context.dockerPresent) {
                    syscmd = getLocalSystem("docker", true);
                } else {
                    syscmd = getLocalSystem(profile.os, false);
                }
                break;
        }
        return syscmd;
    }


    /**
    * We will not collect all the headerst hat are returned by the actions API (kibana_actions.json file).
    * for troubleshooting support engineers will only need "kbn-xsrf" or "Content-Type", all the others are removed.
    *
    * @param  DiagnosticContext context
    * @return void
    */
    public void allowedHeadersFilter(DiagnosticContext context) {
        JsonNode actions = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_actions.json");
        Boolean headerRemoved = false;
        if (actions.size() > 0) {
            for (int i = 0; i < actions.size(); i++) {
                JsonNode config = actions.get(i).get("config");
                // API webhook format can change, and maybe we have a webhook without config
                if (!(config == null || config.isNull())) {
                    // Not all the webhook need to have headers, so we need to be sure the data was set by the customer.
                    JsonNode headers = actions.get(i).get("config").get("headers");
                    if (!(headers == null || headers.isNull())) {
                        Iterator<Map.Entry<String, JsonNode>> iter = actions.get(i).get("config").get("headers").fields();

                        while (iter.hasNext()) {
                            Map.Entry<String, JsonNode> entry = iter.next();
                            if (!entry.getKey().equals("kbn-xsrf") && !entry.getKey().equals("Content-Type")) {
                                iter.remove();
                                headerRemoved = true;
                            }
                        }
                    }
                }
            }
            if (headerRemoved == true) {
                String fileName = context.tempDir + SystemProperties.fileSeparator + "kibana_actions.json";
                try (FileWriter fileWriter = new FileWriter(fileName)) {
                    fileWriter.write(actions.toPrettyString());
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                  logger.error("Unexpected error while writing [kibana_actions.json]", e);
                }
            }
        }
    }


    /**
    * One of the requirements was test this new Kibana code.
    * I splitted the "execute" function as in other examples (RunLogstashQueries) uses many static calls
    * and create many new objects, make it difficult to mock and test.
    * To be able to test the code without changing the global structure I splitted in smallest functions.
    * Most of the new functions are public so I'm able to test it (Private if the test need to be done in a different class).
    * When we will have more Unit test, the test for some functions here will be redundant and the public functions workaround will not needed anymore, 
    * e.g. createJsonNodeFromFileName, this can be tested on the JsonYamlUtilsTest file (not existing in v8.1.2).
    * 
    *
    * @param  DiagnosticContext context
    * @return         JavaPlatform object
    */
    public void execute(DiagnosticContext context) {

        try {
            context.perPage         = 100;
            RestClient client       = ResourceCache.getRestClient(Constants.restInputHost);
            int totalRetries        = runBasicQueries(client, context);
            allowedHeadersFilter(context);
            execSystemCommands(context);

        } catch (Exception e) {
            logger.error("Kibana Query error:", e);
            throw new DiagnosticException(String.format("Error obtaining Kibana output and/or process id - will bypass the rest of processing. %s", Constants.CHECK_LOG));
        }
    }


}
