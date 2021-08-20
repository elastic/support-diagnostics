/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.Constants;
import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.diagnostics.ProcessProfile;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.rest.RestClient;
import co.elastic.support.rest.RestEntry;
import co.elastic.support.rest.RestResult;
import co.elastic.support.util.JsonYamlUtils;
import co.elastic.support.util.LocalSystem;
import co.elastic.support.util.ResourceCache;
import co.elastic.support.util.SystemCommand;
import co.elastic.support.util.SystemProperties;
import co.elastic.support.util.SystemUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileWriter;
import java.io.IOException;

/**
 * RunKibanaQueries executes the version-dependent REST API calls against Kibana.
 */
public class RunKibanaQueries extends BaseQuery {

    private static final Logger logger = LogManager.getLogger(RunKibanaQueries.class);
    // list of spaces defined in Kibana
    private List<String> allSpaces = new ArrayList<>();

   /**
    * Create a new ProcessProfile object and extract the information from fileName to get PID and OS.
    * @param  tempDir where is the temporary data stored
    * @param  fileName of the API content required, for more information check the kibana-rest.ymal
    * @param  context The current diagnostic context as set in the DiagnosticService class
    * @return the object that consolidate the process information
    */
    private ProcessProfile getProfile(String tempDir, String fileName, DiagnosticContext context) {
        ProcessProfile profile = new ProcessProfile();
        context.targetNode = profile;
        JsonNode kibanaData = JsonYamlUtils.createJsonNodeFromFileName(tempDir, fileName);
        profile.pid = kibanaData.path("process").path("pid").asText();
        profile.os = SystemUtils.parseOperatingSystemName(kibanaData.path("os").path("platform").asText());

        return profile;
    }


   /**
    * CheckKibanaVersion (executed before) defined/set the context.elasticRestCalls.
    * here we will loop and create a List of RestEntry with the queries/APIs that need to be executed
    * You have two APIs that work differently and and may have or not many pages, so we use the getAllPages function
    * runQueries called in this function create a json file for each RestEntry set on the 'queries' variable.
    *
    * @param  client the configured client to connect to Kibana.
    * @param  context  The current diagnostic context as set in the DiagnosticService class
    * @return Number of HTTP request that will be executed.
    */
    public int runBasicQueries(RestClient client, DiagnosticContext context) throws DiagnosticException {
        int totalRetries = 0;
        List<RestEntry> queries = new ArrayList<>();
        
        for (Map.Entry<String, RestEntry> entry : context.elasticRestCalls.entrySet()) {

            String actionName = entry.getValue().getName().toString();
            if (actionName.equals("kibana_alerts") ||
                actionName.equals("kibana_actions") || 
                actionName.equals("kibana_alerts_health") || 
                actionName.equals("kibana_detection_engine_find")) {
                runActionsBySpace(client, queries, context.perPage, entry.getValue());
            } else {
                queries.add(entry.getValue());
            }
        }
        totalRetries = runQueries(client, queries, context.tempDir, 0, 0);

        return totalRetries;
    }

   /**
    * Add a new RestEntry action for each API and space
    * we will also update the URL and name with the space id
    *
    * @param  client the configured client to connect to Kibana.
    * @param  queries we will store the list of queries that need to be executed
    * @param  perPage  Number of docusment we reques to the API
    * @param  action Kibana API name we are running
    */
    public void runActionsBySpace(RestClient client, List<RestEntry> queries, int perPage, RestEntry action) throws DiagnosticException {
        List<String> spaces = getAllSpaces(client);
        String url = action.getUrl();
        String baseUrl = url.substring(4, url.length());
        String defaultName = action.getName();
        String defaultUrl = action.getUrl();

        spaces.forEach((spaceId) -> {

            //reset the actionUrl with the original value of action url
            String actionUrl = defaultUrl;
            if (!spaceId.equals("default")) {
                actionUrl = String.format("/s/%s/api"+ baseUrl, spaceId);
            }

            if (action.getUrl().contains("/_find")) {
                try {
                    getAllPages(client, queries, perPage, newEntryWithNameAndUrl(action, defaultName, actionUrl), spaceId);
                } catch (DiagnosticException e) {
                    logger.error(String.format("Space: Unable to page through [%s]", spaceId));
                }
            } else {
                String actionName = defaultName + "_" + spaceId;
                queries.add(newEntryWithNameAndUrl(action, actionName, actionUrl));
            }
        });
    }


   /**
    * On this function we will use the RestEntry action object to get the URL and execute the API one first time
    * to get the total of events defined in Kibana for that API.
    * Once we have the total of event we can calculate the number of pages/call we will need to execute
    * then we call getNewEntryPage to create a new RestEntry for each page.
    *
    * @param  client the configured client to connect to Kibana.
    * @param  queries we will store the list of queries that need to be executed 
    * @param  perPage  Number of docusment we reques to the API
    * @param  action Kibana API name we are running
    * @param  spaceId each space has an id that allow us to collect his data
    */
    public void getAllPages(RestClient client, List<RestEntry> queries, int perPage, RestEntry action, String spaceId) throws DiagnosticException {
        // get the values needed to the pagination.
        RestResult res = client.execQuery(String.format("%s?per_page=1", action.getUrl()));
        if (!res.isValid()) {
            throw new DiagnosticException(res.formatStatusMessage(
                "Could not retrieve Kibana API pagination - unable to continue. "+action.getUrl()
                ));
        }
        String result   = res.toString();
        JsonNode root   = JsonYamlUtils.createJsonNodeFromString(result);
        int total       = root.path("total").intValue();
        if (total > 0 && perPage > 0) {
            // Get the first actions page
            queries.add(getNewEntryPage(perPage, 1, action, spaceId));
            // If there is more pages add the new queries
            if (perPage < total) {
                int numberPages = (int)Math.ceil(total / perPage);
                for (int i = 2; i <= numberPages; i++) {
                    queries.add(getNewEntryPage(perPage, i, action, spaceId));
                }
            }
        }
    }


   /**
    * Retrieve all kibana spaces available for current process/instance
    * the results are stored in a global vairable, we do this to cache the result
    * the objective is to reduce the executions of the spaces API
    *
    * @param client the configured client to connect to Kibana.
    * @return all the spaces name.
    */
    private List<String> getAllSpaces(RestClient client) throws DiagnosticException {

        // by default Kibana has one space, if allSpaces is empty it means we have not yet run this API.
        if (this.allSpaces.size() <= 0) {
            RestResult res = client.execQuery("/api/spaces/space");
            if (!res.isValid()) {
                logger.error("Could not retrieve Kibana spaces, we will collect data only for default space.");
            } else {
                String result   = res.toString();
                JsonNode spaces   = JsonYamlUtils.createJsonNodeFromString(result);
                if (spaces.size() > 0) {
                    for (int i = 0; i < spaces.size(); i++) {
                        JsonNode space = spaces.get(i);
                        // API webhook format can change, and maybe we have a webhook without config
                        if (!(space == null || space.isNull())) {
                            // Not all the webhook need to have headers, so we need to be sure the data was set by the customer.
                            String name = space.get("id").toString();
                            if (!(name.equals(null) || name.equals(""))) {
                                this.allSpaces.add(name.replace("\"",""));
                            }
                        }
                    }
                }
            }
        }
        return this.allSpaces;
    }

   /**
    * create a new `RestEntry` with the proper querystring parameters for paging.
    *
    * @param  perPage how many events need to be retreived in the response
    * @param  page the apge we are requesting
    * @param  action Kibana API name we are running
    * @param  spaceId Spaces has an id that allow us to collect the data for each space
    * @return new object with the API and params that need to be executed.
    */
    private RestEntry getNewEntryPage(int perPage, int page, RestEntry action, String spaceId) {
        return new RestEntry(String.format("%s_%s_%s", action.getName(), spaceId, page), "", ".json", false, String.format("%s?per_page=%s&page=%s", action.getUrl(), perPage, page), false);
    }

   /**
    * Create a new `RestEntry` object with a different name and URL of the current {@code RestEntry}.
    *
    * @param restEntry original object
    * @param name The new name to use
    * @param url The new URL to use
    * @return A copy of the current {@code RestEntry} with the {@code name} and {@code url}.
    */
    private RestEntry newEntryWithNameAndUrl(RestEntry restEntry, String name, String url) {
        return new RestEntry(name, restEntry.getSubdir(), restEntry.getExtension(), restEntry.isRetry(), url, false);
    }


   /**
    * create a LocalSystem object as Kibana is running in a docker container
    *
    * @return LocalSytem that will allow us to communicate with docker
    */
    private LocalSystem getDockerSystem() {
        String osName = SystemUtils.parseOperatingSystemName(SystemProperties.osName);
        LocalSystem syscmd = new LocalSystem(osName);
        ResourceCache.addSystemCommand(Constants.systemCommands, syscmd);
        return syscmd;
    }

   /**
    * This function is executed **after** runBasicQueries
    * Extract the information on the kibana_stats.json with getProfile function.
    * set the ResourceCache.addSystemCommand according tp the OS 
    *
    * @param  context The current diagnostic context as set in the DiagnosticService class
    * @return according with the type of diagnostic return the system command
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
                
                syscmd = ResourceCache.getSystemCommand(Constants.systemCommands);
                break;

            case Constants.kibanaLocal:
                if (context.dockerPresent) {
                    syscmd = getDockerSystem();
                } else {
                    syscmd = ResourceCache.getSystemCommand(Constants.systemCommands);
                }
                break;
        }
        return syscmd;
    }


   /**
    * We will not collect all the headerst hat are returned by the actions API (kibana_actions.json file).
    * for troubleshooting support engineers will only need "kbn-xsrf" or "Content-Type", all the others are removed.
    *
    * @param client the configured client to connect to Kibana.
    * @param context The current diagnostic context as set in the DiagnosticService class
    */
    public void filterActionsHeaders(RestClient client, DiagnosticContext context) throws DiagnosticException {

        List<String> spaces = getAllSpaces(client);

        spaces.forEach((spaceId) -> {
            try {
                JsonNode actions = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir,
                                                                    "kibana_actions_"+ spaceId +".json");
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
                                    String key = entry.getKey().toLowerCase();

                                    if (!key.equals("kbn-xsrf") && !key.equals("content-type")) {
                                        iter.remove();
                                        headerRemoved = true;
                                    }
                                }
                            }
                        }
                    }

                    // If any headers were removed, we need to rewrite the file to remove them
                    if (headerRemoved == true) {
                        Path path = Paths.get(context.tempDir, "kibana_actions_" + spaceId + ".json");
                        try (FileWriter fileWriter = new FileWriter(path.toFile())) {
                            fileWriter.write(actions.toPrettyString());
                            fileWriter.flush();
                        } catch (IOException e) {
                          logger.error("Unexpected error while writing [kibana_actions_"+ spaceId +".json]", e);
                        }
                    }
                }
            } catch (RuntimeException e) {
                logger.error("Kibana actions file is empty, we have nothing to filter.", e);
            }
        });
    }


   /**
    * According with the version we will run the Kibana APIs, and store the data.
    * We will also try to collect the sysstat for the OS and the kibana logs if possible.
    *
    * @param  context The current diagnostic context as set in the DiagnosticService class
    */
    public void execute(DiagnosticContext context) throws DiagnosticException {

        try {
            context.perPage         = 100;
            RestClient client       = ResourceCache.getRestClient(Constants.restInputHost);
            int totalRetries        = runBasicQueries(client, context);
            filterActionsHeaders(client, context);
            execSystemCommands(context);

        } catch (Exception e) {
            logger.error("Kibana Query error:", e);
            throw new DiagnosticException(String.format("Error obtaining Kibana output and/or process id - will bypass the rest of processing. %s", Constants.CHECK_LOG));
        }
    }


}
