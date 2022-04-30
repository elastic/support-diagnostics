/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.diagnostics.DiagConfig;
import co.elastic.support.diagnostics.DiagnosticInputs;
import co.elastic.support.util.JsonYamlUtils;
import co.elastic.support.util.ResourceCache;
import co.elastic.support.util.SystemProperties;
import co.elastic.support.Constants;
import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.util.SystemUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.rest.RestEntryConfig;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Parameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.*;
import co.elastic.support.rest.RestClient;
import co.elastic.support.rest.RestEntry;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRunKibanaQueries {

    private static final Logger logger = LogManager.getLogger(RestClient.class);
    private ClientAndServer mockServer;
    private RestClient httpRestClient, httpsRestClient;
    private String temp = SystemProperties.userDir + SystemProperties.fileSeparator + "temp";
    private File tempDir = new File(temp);
    private ResourceCache resourceCache;

    @BeforeAll
    public void globalSetup() {
        mockServer = startClientAndServer(9880);
    }

    @AfterAll
    public void globalTeardoown() {
        mockServer.stop();
    }

    @BeforeEach
    public void setup() {

        httpRestClient = RestClient.getClient(
            "localhost",
            9880,
            "http",
            "elastic",
            "elastic",
            "",
            0,
            "",
            "",
            "",
            "",
            true,
            Collections.emptyMap(),
           3000,
           3000,
           3000);
        tempDir.mkdir();

        resourceCache = new ResourceCache();
    }

    @AfterEach
    public void tearDown() {

        mockServer.reset();
        FileUtils.deleteQuietly(tempDir);
        resourceCache.close();
    }

    private void configMockServerRoute(String path, String body, Parameter... params) {
        mockServer.when(
          request()
            .withMethod("GET")
            .withPath(path)
            .withQueryStringParameters(params)
        ).respond(
          response()
            .withBody(body)
            .withStatusCode(200)
        );
    }

    private DiagnosticContext initializeKibana(String version) throws DiagnosticException {
        Map diagMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
        DiagConfig diagConfig = new DiagConfig(diagMap);
        DiagnosticContext context = new DiagnosticContext(diagConfig, new DiagnosticInputs("testKibana"), resourceCache, true);
        RestEntryConfig builder = new RestEntryConfig(version);

        Map restCalls = JsonYamlUtils.readYamlFromClasspath(Constants.KIBANA_REST, true);
        Map<String, RestEntry> entries = builder.buildEntryMap(restCalls);
        context.elasticRestCalls = entries;

        context.tempDir = tempDir.getPath();
        context.perPage = 2;

        configMockServerRoute(
          "/api/settings",
          "{\"cluster_uuid\":\"RLtzkhfBRUadN4WZ8fnnog\",\"settings\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"6.5.0\",\"snapshot\":false,\"status\":\"green\"}}}"
        );
        configMockServerRoute(
          "/api/stats",
          "{\"process\":{\"memory\":{\"heap\":{\"total_bytes\":470466560,\"used_bytes\":343398224,\"size_limit\":1740165498},\"resident_set_size_bytes\":587878400},\"pid\":32,\"event_loop_delay\":0.280181884765625,\"uptime_ms\":97230924},\"os\":{\"platform\":\"linux\",\"platform_release\":\"linux-4.15.0-1032-gcp\",\"load\":{\"1m\":1.37451171875,\"5m\":1.43408203125,\"15m\":1.34375},\"memory\":{\"total_bytes\":147879931904,\"free_bytes\":45620334592,\"used_bytes\":102259597312},\"uptime_ms\":18093713000,\"distro\":\"Centos\",\"distro_release\":\"Centos-7.8.2003\"},\"requests\":{\"disconnects\":0,\"total\":1,\"status_codes\":{\"302\":1}},\"concurrent_connections\":8,\"timestamp\":\"2021-01-06T01:35:11.324Z\",\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"7.9.0\",\"snapshot\":false,\"status\":\"green\"},\"last_updated\":\"2021-01-06T01:35:15.911Z\",\"collection_interval_ms\":5000,\"cluster_uuid\":\"RfBRUssssadN4WZssnnog\"}"
        );
        configMockServerRoute(
          "/api/actions",
          "[{\"id\":\"10c09567-fa28-4059-9484-aae83fa9c5ce\",\"actionTypeId\":\".webhook\",\"name\":\"rerere\",\"config\":{\"method\":\"post\",\"url\":\"https://mail.google.com/mail/u/0/#inbox\"}},{\"id\":\"eec7ee50-7129-11eb-9b41-17a1879ebc72\",\"actionTypeId\":\".webhook\",\"name\":\"webhook-auto-create-case\",\"config\":{\"method\":\"post\",\"hasAuth\":true,\"url\":\"https://7067a1cd7a8142a79ab8dec9304a5436.europe-west1.gcp.cloud.es.io/api/cases\",\"headers\":{\"kbn-xsrf\":\"kibana\",\"Content-Type\":\"application/json\"}},\"isPreconfigured\":false,\"referencedByCount\":8}]"
        );
        configMockServerRoute(
          "/api/detection_engine/privileges",
          "{\"username\":\"elastic\",\"has_all_requested\":false,\"is_authenticated\":true,\"has_encryption_key\":true}"
        );
        configMockServerRoute(
          "/api/detection_engine/prepackaged",
          "{\"username\":\"elastic\"}"
        );
        configMockServerRoute(
          "/api/task_manager/_health",
          "{\"username\":\"elastic\"}"
        );
        configMockServerRoute(
          "/api/alerts/_find",
          "{\"page\":1,\"perPage\":2,\"total\":4,\"data\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"6.5.0\",\"snapshot\":false,\"status\":\"green\"}}}",
          new Parameter("per_page", "1")
        );
        configMockServerRoute(
          "/api/alerts/_find",
          "{\"page\":1,\"perPage\":2,\"total\":4,\"data\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"6.5.0\",\"snapshot\":false,\"status\":\"green\"}}}",
          new Parameter("page", "1"),
          new Parameter("per_page", "2")
        );
        configMockServerRoute(
          "/api/alerts/_find",
          "{\"page\":2,\"perPage\":2,\"total\":4,\"data\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"6.5.0\",\"snapshot\":false,\"status\":\"green\"}}}",
          new Parameter("page", "2"),
          new Parameter("per_page", "2")
        );
        configMockServerRoute(
          "/api/detection_engine/rules/_find",
          "{\"page\":1,\"perPage\":2,\"total\":4,\"data\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"6.5.0\",\"snapshot\":false,\"status\":\"green\"}}}",
          new Parameter("per_page", "1")
        );
        configMockServerRoute(
          "/api/detection_engine/rules/_find",
          "{\"page\":1,\"perPage\":2,\"total\":4,\"data\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"6.5.0\",\"snapshot\":false,\"status\":\"green\"}}}",
          new Parameter("page", "1"),
          new Parameter("per_page", "2")
        );
        configMockServerRoute(
          "/api/detection_engine/rules/_find",
          "{\"page\":2,\"perPage\":2,\"total\":4,\"data\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"6.5.0\",\"snapshot\":false,\"status\":\"green\"}}}",
          new Parameter("page", "2"),
          new Parameter("per_page", "2")
        );

        return context;
    }

    @Test
    public void testQueriesForKibanaWithHeaders() throws DiagnosticException {
        DiagnosticContext context = initializeKibana("7.10.0");
        int totalRetries = new RunKibanaQueries().runBasicQueries(httpRestClient, context);
        new RunKibanaQueries().filterActionsHeaders(context);

        JsonNode nodeData = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_actions.json");

        JsonNode headers = nodeData.get(0).get("config").get("headers");
        assertEquals(nodeData.get(0).path("id").asText(), "10c09567-fa28-4059-9484-aae83fa9c5ce");
        assertTrue((headers == null || headers.isNull()));

        assertEquals(nodeData.get(1).path("id").asText(), "eec7ee50-7129-11eb-9b41-17a1879ebc72");
        assertEquals(nodeData.get(1).path("config").path("headers").size(), 2);
        assertEquals(nodeData.get(1).path("config").path("headers").path("kbn-xsrf").asText(), "kibana");
        assertEquals(nodeData.get(1).path("config").path("headers").path("Content-Type").asText(), "application/json");

    }

    @Test
    public void testQueriesRemovingHeaders() throws DiagnosticException {
        configMockServerRoute(
          "/api/actions",
          "[{\"id\":\"eec7ee50-7129-1111-9b41-17a1879ebc72\",\"actionTypeId\":\".webhook\",\"name\":\"webhook-auto-create-case\",\"config\":{\"method\":\"post\",\"hasAuth\":true,\"url\":\"https://7067a1cd7a8142a79ab8dec9304a5436.europe-west1.gcp.cloud.es.io/api/cases\",\"headers\":{\"Authorization\":\"Basic 1111\",\"customHeader\":\"CustomValue\",\"kbn-xsrf\":\"kibana\",\"Content-Type\":\"application/json\"}},\"isPreconfigured\":false,\"referencedByCount\":9}]"
        );

        DiagnosticContext context = initializeKibana("7.10.0");
        int totalRetries = new RunKibanaQueries().runBasicQueries(httpRestClient, context);
        new RunKibanaQueries().filterActionsHeaders(context);

        JsonNode nodeData = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_actions.json");
        String id = nodeData.get(0).path("id").asText();
        assertEquals(id, "eec7ee50-7129-1111-9b41-17a1879ebc72");
        assertEquals(nodeData.get(0).path("config").path("headers").size(), 2);
        assertEquals(nodeData.get(0).path("config").path("headers").path("kbn-xsrf").asText(), "kibana");
        assertEquals(nodeData.get(0).path("config").path("headers").path("Content-Type").asText(), "application/json");
    }

   /**
    * This test is to be sure we have no issues in case the webhook come with no config or headers
    */
    @Test
    public void testQueriesWithoutHeaders() throws DiagnosticException {
        configMockServerRoute(
          "/api/actions",
          "[{\"id\":\"eec7ee50-7129-3333-9b41-17a1879ebc72\",\"actionTypeId\":\".webhook\",\"name\":\"webhook-auto-create-case\",\"test\":{\"method\":\"post\",\"hasAuth\":true,\"url\":\"https://7067a1cd7a8142a79ab8dec9304a5436.europe-west1.gcp.cloud.es.io/api/cases\",\"headers\":{\"Authorization\":\"Basic 1111\",\"customHeader\":\"CustomValue\",\"kbn-xsrf\":\"kibana\",\"Content-Type\":\"application/json\"}},\"isPreconfigured\":false,\"referencedByCount\":9}]"
        );

        DiagnosticContext context = initializeKibana("7.10.0");
        int totalRetries = new RunKibanaQueries().runBasicQueries(httpRestClient, context);

        // mutates `context` and throws an error uppon failure
        new RunKibanaQueries().filterActionsHeaders(context);

        JsonNode nodeData = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_actions.json");
        JsonNode config = nodeData.get(0).get("config");
        assertEquals(nodeData.get(0).path("id").asText(), "eec7ee50-7129-3333-9b41-17a1879ebc72");
        assertTrue((config == null || config.isNull()));
    }
}
