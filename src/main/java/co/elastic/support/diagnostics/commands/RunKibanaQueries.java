/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;

/**
 * RunKibanaQueries executes the version-dependent REST API calls against Kibana.
 */
public class RunKibanaQueries extends BaseQuery {

    private static final Logger logger = LogManager.getLogger(RunKibanaQueries.class);

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

    private List<String> getKibanaSpacesIds(DiagnosticContext context) throws DiagnosticException {
        RestClient restClient = context.resourceCache.getRestClient(Constants.restInputHost);
        String url = context.fullElasticRestCalls.get("kibana_spaces").getUrl();
        RestResult result = restClient.execQuery(url);
        
        if (result.getStatus() != 200) {
            throw new DiagnosticException(String.format(
                "Kibana responded with [%d] for [%s]. Unable to proceed.",
                result.getStatus(), url));
        }

        JsonNode spacesResponse = JsonYamlUtils.createJsonNodeFromString(result.toString());
        if (!spacesResponse.isArray()) {
            throw new DiagnosticException("Kibana Spaces API returned an invalid response. A list of Spaces was expected.");
        }
        ArrayNode arrayNode = (ArrayNode) spacesResponse;
        List<String> spacesIds = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            JsonNode idNode = node.path("id");
            if(!idNode.isMissingNode()) {
                spacesIds.add(idNode.asText());
            }
        }
        return spacesIds;
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
    public int runBasicQueries(RestClient client, DiagnosticContext context, List<String> spacesIds) throws DiagnosticException {
        List<RestEntry> queries = new ArrayList<>();

        for (Map.Entry<String, RestEntry> entry : context.elasticRestCalls.entrySet()) {
            RestEntry current = entry.getValue();
            if (current.isSpaceAware()) {
                for (String spaceId : spacesIds) {
                    RestEntry restEntry = current;

                    // The calls made for the default Space will be written without a subpath
                    if (!"default".equals(spaceId)) {
                        String url = String.format("/s/%s%s", spaceId, restEntry.getUrl());
                        String subdir = Paths.get(
                            restEntry.getSubdir(),
                            "space_" + spaceId.replaceAll("[^a-zA-Z0-9-_]", "_")
                        ).normalize().toString();

                        restEntry = current.copyWithNewUrl(url, subdir);
                    }

                    if (restEntry.isPageable()) {
                        getAllPages(client, queries, context.perPage, restEntry, current.getPageableFieldName());
                    } else {
                        queries.add(restEntry);
                    }
                }
            } else if (current.isPageable()) {
                getAllPages(client, queries, context.perPage, entry.getValue(), current.getPageableFieldName());
            } else {
                queries.add(entry.getValue());
            }
        }

        return runQueries(client, queries, context.tempDir, 0, 0);
    }

    private String getPageUrl(RestEntry action, int page, int perPage, String perPageField) {
        String actionUrl = action.getUrl();

        final String querystringPrefix = URI.create(actionUrl).getQuery() != null ? "&" : "?";
        final String params = "page=" + page + "&" + perPageField + "=" + perPage;

        return actionUrl + querystringPrefix + params;
    }

    /**
     * On this function we will use the RestEntry action object to get the URL and execute the API one first time
     * to get the total of events defined in Kibana for that API.
     * Once we have the total of event we can calculate the number of pages/call we will need to execute
     * then we call getNewEntryPage to create a new RestEntry for each page.
     *
     * @param client the configured client to connect to Kibana.
     * @param queries we will store the list of queries that need to be executed
     * @param perPage  Number of docusment we reques to the API
     * @param action Kibana API name we are running
     * @param perPageField 
     */
    public void getAllPages(RestClient client, List<RestEntry> queries, int perPage, RestEntry action, String perPageField) throws DiagnosticException {
        // get the values needed to the pagination (only need the total)
        String url = getPageUrl(action, 1, 1, perPageField);

        // get the values needed to the pagination.
        RestResult res = client.execQuery(url);

        int totalPages = 0;

        if (res.isValid()) {
            JsonNode root = JsonYamlUtils.createJsonNodeFromString(res.toString());
            totalPages = (int)Math.ceil(root.path("total").doubleValue() / perPage);
        }

        // guarantee at least one page is returned regardless of total
        queries.add(getNewEntryPage(perPage, 1, action, perPageField));

        if (totalPages > 1) {
            for (int currentPage = 2; currentPage <= totalPages; ++currentPage) {
                queries.add(getNewEntryPage(perPage, currentPage, action, perPageField));
            }
        }
    }

   /**
    * create a new `RestEntry` with the proper querystring parameters for paging.
    *
    * @param  perPage how many events need to be retreived in the response
    * @param  page the apge we are requesting
    * @param  action Kibana API name we are running
    * @param perPageField 
    * @return new object with the API and params that need to be executed.
    */
    private RestEntry getNewEntryPage(int perPage, int page, RestEntry action, String perPageField) {
        return new RestEntry(
            String.format("%s_%s", action.getName(), page),
            action.getSubdir(),
            action.getExtension(),
            false,
            getPageUrl(action, page, perPage, perPageField),
            false
        );
    }


   /**
    * create a LocalSystem object as Kibana is running in a docker container
    *
    * @return LocalSytem that will allow us to communicate with docker
    */
    private LocalSystem getDockerSystem(ResourceCache resourceCache) {
        String osName = SystemUtils.parseOperatingSystemName(SystemProperties.osName);
        LocalSystem syscmd = new LocalSystem(osName);
        resourceCache.addSystemCommand(Constants.systemCommands, syscmd);
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

                syscmd = context.resourceCache.getSystemCommand(Constants.systemCommands);
                break;

            case Constants.kibanaLocal:
                if (context.dockerPresent) {
                    syscmd = getDockerSystem(context.resourceCache);
                } else {
                    syscmd = context.resourceCache.getSystemCommand(Constants.systemCommands);
                }
                break;
        }
        return syscmd;
    }


   /**
    * We will not collect all the headers that are returned by the actions API (kibana_actions.json file).
    * for troubleshooting support engineers will only need "kbn-xsrf" or "Content-Type", all the others are removed.
    *
    * @param  context The current diagnostic context as set in the DiagnosticService class
    */
    public void filterActionsHeaders(DiagnosticContext context) {

        try {
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
                    String fileName = context.tempDir + SystemProperties.fileSeparator + "kibana_actions.json";
                    try (FileWriter fileWriter = new FileWriter(fileName)) {
                        ObjectMapper mapper = new ObjectMapper();
                        fileWriter.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(actions));
                        fileWriter.flush();
                    } catch (IOException e) {
                      logger.error("Unexpected error while writing [kibana_actions.json]", e);
                    }
                }
            }
        } catch (RuntimeException e) {
            logger.error("Kibana actions file is empty, we have nothing to filter.", e);
        }
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
            RestClient client       = context.resourceCache.getRestClient(Constants.restInputHost);
            List<String> spacesIds  = getKibanaSpacesIds(context);
            int totalRetries        = runBasicQueries(client, context, spacesIds);
            filterActionsHeaders(context);
            execSystemCommands(context);

        } catch (Exception e) {
            logger.error("Kibana Query error:", e);
            throw new DiagnosticException(String.format("Error obtaining Kibana output and/or process id - will bypass the rest of processing. %s", Constants.CHECK_LOG));
        }
    }


}
