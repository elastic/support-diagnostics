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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *  This class is executed as RunKibanaQueries class, we will not change the global BaseQuery structure of the code in v8.7.3.
 *  As unit test are request, we have done some changes to the structure of this class vs RunLogstashQueries.
 *  TODO: To be able to test the RunKibanaQueries functions I will need to split the execute function and create new public or private functions.
 *  In the next version we will need to work in a different pattern to remove the workarounds that were done here to test the new Kibana code.
 */

public class RunKibanaQueries extends BaseQuery {

    /**
     * Executes the REST calls for Kibana
     */

    private static final Logger logger = LogManager.getLogger(BaseQuery.class);

    /**
    * Create a new ProcessProfile object and extract the information from fileName to get PID, OS and javaPlatform
    *
    * @param  String tempDir
    * @param  String fileName
    * @param  DiagnosticContext context
    * @return         ProcessProfile object
    */
    private ProcessProfile getNodeProfile(String tempDir, String fileName, DiagnosticContext context) {
        ProcessProfile nodeProfile = new ProcessProfile();
        context.targetNode = nodeProfile;
        JsonNode nodeData = JsonYamlUtils.createJsonNodeFromFileName(tempDir, fileName);
        nodeProfile.pid = nodeData.path("process").path("pid").asText();
        nodeProfile.os = SystemUtils.parseOperatingSystemName(nodeData.path("os").path("platform").asText());
        nodeProfile.javaPlatform = getJavaPlatformOs(nodeProfile.os);

        return nodeProfile;
    }

    /**
    * this private function will create a new JavaPlatform object
    *
    * @param  String nodeProfileOs
    * @return         JavaPlatform object
    */
    private JavaPlatform getJavaPlatformOs(String nodeProfileOs) {
        JavaPlatform javaPlatformOs = new JavaPlatform(nodeProfileOs);
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
    *
    * @return         void
    */
    public void getAllPages(RestClient client, List<RestEntry> queries, double perPage, RestEntry action) {
        // get the values needed to the pagination.
        RestResult res = client.execQuery(String.format("%s?per_page=1", action.getUrl()));
        if (! res.isValid()) {
            throw new DiagnosticException( res.formatStatusMessage( "Could not retrieve Kibana API pagination - unable to continue."));
        }
        String result   = res.toString();
        JsonNode root   = JsonYamlUtils.createJsonNodeFromString(result);
        double total    = root.path("total").intValue();
        if (total > 0 && perPage > 0) {
            // Get the first actions page
            queries.add(getNewEntryPage(perPage, 1, action));
            // If there is more pages add the new queries
            if (perPage < total) {
                double numberPages = Math.ceil(total/perPage);
                for (int i = 2; i <= numberPages; i++) {
                    queries.add(getNewEntryPage(perPage, i, action));
                }
            }
        }
    }

    /**
    * create a new RestEntry page for the original RestEntry action 
    *
    * @param  int perPage
    * @param  int page
    * @param  RestEntry action
    * @return RestEntry
    */
    private RestEntry getNewEntryPage(double perPage, int page, RestEntry action) {
        return new RestEntry(String.format("%s_%s", action.getName(), page), "", ".json", false, String.format("%s?per_page=%s&page=%s", action.getUrl(), perPage, page), false);
    }


    /**
    * This function is executed **after** runBasicQueries
    * Extract the information on the kibana_node_stats.json with getNodeProfile function.
    * Within the function getRemoteSystem or getLocalSystem we set ResourceCache.addSystemCommand
    * Return will be used as workaround to Unit test. Will be replaced in v9
    *
    * @param  DiagnosticContext
    * @return SystemCommand
    */
    public SystemCommand execSystemCommands(DiagnosticContext context) {

        ProcessProfile nodeProfile = getNodeProfile(context.tempDir, "kibana_node_stats.json", context);

        if (StringUtils.isEmpty(nodeProfile.pid) || nodeProfile.pid.equals("1")) {
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
                    targetOS = nodeProfile.os;
                }
                
                syscmd = getRemoteSystem(targetOS, context);
                break;

            case Constants.kibanaLocal:
                if (context.dockerPresent) {
                    syscmd = getLocalSystem("docker", true);
                } else {
                    syscmd = getLocalSystem(nodeProfile.os, false);
                }
                break;
        }
        return syscmd;
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
            execSystemCommands(context);

        } catch (Throwable t) {
            logger.error( "Kibana Query error:", t);
            throw new DiagnosticException(String.format("Error obtaining Kibana output and/or process id - will bypass the rest of processing.. %s", Constants.CHECK_LOG));
        }
    }


}