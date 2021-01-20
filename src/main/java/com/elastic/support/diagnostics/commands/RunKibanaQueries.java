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
 *  This class is executed as RunLogstashQueries class, we will not change the global BaseQuery structure of the code in v8.7.3.
 *  As unit test are request, we have done some changes to the structure of this class vs RunLogstashQueries.
 *  TODO: The RunLogstashQueries class start elements into the execute function, to be able to test the RunKibanaQueries functions I will need to create new public functions.
 *  The right way to do is create private functions, but this functions can not be mock (with Mockito) and I don't want to use PowerMock because how the code was designed.
 *  In the next version I will work in a new Factoy pattern to remove the workarounds that were done here to test the new Kibana code.
 */

public class RunKibanaQueries extends BaseQuery {

    /**
     * Executes the REST calls for Kibana
     */

    private static final Logger logger = LogManager.getLogger(BaseQuery.class);

    /**
    * this private function will not be tested (on this class, this need to bested on ProcessProfileTest)
    *
    * @param  void
    * @return         ProcessProfile object
    */
    private ProcessProfile getNodeProfile(String tempDir, String fileName) {
        ProcessProfile nodeProfile = new ProcessProfile();
        JsonNode nodeData = JsonYamlUtils.createJsonNodeFromFileName(tempDir, fileName);
        nodeProfile.pid = nodeData.path("process").path("pid").asText();
        nodeProfile.os = SystemUtils.parseOperatingSystemName(nodeData.path("os").path("platform").asText());
        nodeProfile.javaPlatform = getJavaPlatformOs(nodeProfile.os);

        return nodeProfile;
    }

    /**
    * this private function will not be tested (on this class, this need to bested on JavaPlatformTest)
    *
    * @param  String
    * @return         JavaPlatform object
    */
    private JavaPlatform getJavaPlatformOs(String nodeProfileOs) {
        JavaPlatform javaPlatformOs = new JavaPlatform(nodeProfileOs);
        return javaPlatformOs;
    }

    /**
    * this private function will not be tested (on this class, this need to bested on RemoteSystemTest)
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
    * this private function will not be tested (on this class, this need to bested on LocalSystemTest)
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
    * this public function is a workaround so we can test the main execute function
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
                getAllAlerts(client, queries, context.perPage, entry.getValue());
            } else {
                queries.add(entry.getValue());
            }
        }
        totalRetries = runQueries(client, queries, context.tempDir, 0, 0);

        return totalRetries;
    }


    public void getAllAlerts(RestClient client, List<RestEntry> queries, double perPage, RestEntry action) {
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
    * function that will get the page and the number of events per page to create URI for the _find APIs
    *
    * @param  int perPage
    * @param  int page
    * @return RestEntry
    */
    private RestEntry getNewEntryPage(double perPage, int page, RestEntry action) {
        return new RestEntry(String.format("%s_%s", action.getName(), page), "", ".json", false, String.format("%s?per_page=%s&page=%s", action.getUrl(), perPage, page), false);
    }


    /**
    * this public function is a workaround so we can test the main execute function
    *
    * @param  DiagnosticContext
    * @return         SystemCommand
    */
    public SystemCommand execSystemCommands(DiagnosticContext context) {

        ProcessProfile nodeProfile = getNodeProfile(context.tempDir, "kibana_node_stats.json");

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

/*            default:
                // If it's not one of the above types it shouldn't be here but try to keep going...
                context.runSystemCalls = false;
                throw new RuntimeException("Host/Platform check error.");*/
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
