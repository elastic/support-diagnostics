/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.util.JsonYamlUtils;
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

    }

    @AfterEach
    public void tearDown() {

        mockServer.reset();
        FileUtils.deleteQuietly(tempDir);

    }

    private DiagnosticContext initializeKibana(String version) throws DiagnosticException {

    	DiagnosticContext context = new DiagnosticContext();
    	RestEntryConfig builder = new RestEntryConfig(version);

        Map restCalls = JsonYamlUtils.readYamlFromClasspath(Constants.KIBANA_REST, true);
        Map<String, RestEntry> entries = builder.buildEntryMap(restCalls);
    	context.elasticRestCalls = entries;

    	context.tempDir = tempDir.getPath();
    	context.perPage = 2;
    	
        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/settings")
                )
                .respond(
                        response()
                                .withBody("{\"cluster_uuid\":\"RLtzkhfBRUadN4WZ8fnnog\",\"settings\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"6.5.0\",\"snapshot\":false,\"status\":\"green\"}}}")
                                .withStatusCode(200)
                );

    	mockServer
		        .when(
		                request()
		                        .withMethod("GET")
		                        .withPath("/api/stats")
		        )
		        .respond(
		                response()
		                        .withBody("{\"process\":{\"memory\":{\"heap\":{\"total_bytes\":470466560,\"used_bytes\":343398224,\"size_limit\":1740165498},\"resident_set_size_bytes\":587878400},\"pid\":32,\"event_loop_delay\":0.280181884765625,\"uptime_ms\":97230924},\"os\":{\"platform\":\"linux\",\"platform_release\":\"linux-4.15.0-1032-gcp\",\"load\":{\"1m\":1.37451171875,\"5m\":1.43408203125,\"15m\":1.34375},\"memory\":{\"total_bytes\":147879931904,\"free_bytes\":45620334592,\"used_bytes\":102259597312},\"uptime_ms\":18093713000,\"distro\":\"Centos\",\"distro_release\":\"Centos-7.8.2003\"},\"requests\":{\"disconnects\":0,\"total\":1,\"status_codes\":{\"302\":1}},\"concurrent_connections\":8,\"timestamp\":\"2021-01-06T01:35:11.324Z\",\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"7.9.0\",\"snapshot\":false,\"status\":\"green\"},\"last_updated\":\"2021-01-06T01:35:15.911Z\",\"collection_interval_ms\":5000,\"cluster_uuid\":\"RfBRUssssadN4WZssnnog\"}")
		        );
		mockServer
		        .when(
		                request()
		                        .withMethod("GET")
		                        .withPath("/api/actions")
		        )
		        .respond(
		                response()
		                        .withBody("[{\"id\":\"10c09567-fa28-4059-9484-aae83fa9c5ce\",\"actionTypeId\":\".webhook\",\"name\":\"rerere\",\"config\":{\"method\":\"post\",\"url\":\"https://mail.google.com/mail/u/0/#inbox\"}},{\"id\":\"eec7ee50-7129-11eb-9b41-17a1879ebc72\",\"actionTypeId\":\".webhook\",\"name\":\"webhook-auto-create-case\",\"config\":{\"method\":\"post\",\"hasAuth\":true,\"url\":\"https://7067a1cd7a8142a79ab8dec9304a5436.europe-west1.gcp.cloud.es.io/api/cases\",\"headers\":{\"kbn-xsrf\":\"kibana\",\"Content-Type\":\"application/json\"}},\"isPreconfigured\":false,\"referencedByCount\":8}]")
		        );
		mockServer
		        .when(
		                request()
		                        .withMethod("GET")
		                        .withPath("/api/detection_engine/privileges")
		        )
		        .respond(
		                response()
		                        .withBody("{\"username\":\"elastic\",\"has_all_requested\":false,\"is_authenticated\":true,\"has_encryption_key\":true}")
		        );
		mockServer
		        .when(
		                request()
		                        .withMethod("GET")
		                        .withPath("/api/detection_engine/prepackaged")
		        )
		        .respond(
		                response()
		                        .withBody("{\"username\":\"elastic\"}")
		        );

		mockServer
		        .when(
		                request()
		                        .withMethod("GET")
		                        .withPath("/api/task_manager/_health")
		        )
		        .respond(
		                response()
		                        .withBody("{\"username\":\"elastic\"}")
		        ); 
		mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/alerts/_find")
                                .withQueryStringParameters(
		                                new Parameter("per_page", "1")
		                        )
                )
                .respond(
                        response()
                                .withBody("{\"page\":1,\"perPage\":2,\"total\":4,\"data\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"6.5.0\",\"snapshot\":false,\"status\":\"green\"}}}")
                                .withStatusCode(200)
                );
		mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/alerts/_find")
                                .withQueryStringParameters(
		                                new Parameter("page", "1"),
		                                new Parameter("per_page", "2")
		                        )
                )
                .respond(
                        response()
                                .withBody("{\"page\":1,\"perPage\":2,\"total\":4,\"data\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"6.5.0\",\"snapshot\":false,\"status\":\"green\"}}}")
                                .withStatusCode(200)
                );
        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/alerts/_find")
                                .withQueryStringParameters(
		                                new Parameter("page", "2"),
		                                new Parameter("per_page", "2")
		                        )
                )
                .respond(
                        response()
                                .withBody("{\"page\":2,\"perPage\":2,\"total\":4,\"data\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"6.5.0\",\"snapshot\":false,\"status\":\"green\"}}}")
                                .withStatusCode(200)
                );

        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/detection_engine/rules/_find")
                                .withQueryStringParameters(
		                                new Parameter("per_page", "1")
		                        )
                )
                .respond(
                        response()
                                .withBody("{\"page\":1,\"perPage\":2,\"total\":4,\"data\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"6.5.0\",\"snapshot\":false,\"status\":\"green\"}}}")
                                .withStatusCode(200)
                );
		mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/detection_engine/rules/_find")
                                .withQueryStringParameters(
		                                new Parameter("page", "1"),
		                                new Parameter("per_page", "2")
		                        )
                )
                .respond(
                        response()
                                .withBody("{\"page\":1,\"perPage\":2,\"total\":4,\"data\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"6.5.0\",\"snapshot\":false,\"status\":\"green\"}}}")
                                .withStatusCode(200)
                );
        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/detection_engine/rules/_find")
                                .withQueryStringParameters(
		                                new Parameter("page", "2"),
		                                new Parameter("per_page", "2")
		                        )
                )
                .respond(
                        response()
                                .withBody("{\"page\":2,\"perPage\":2,\"total\":4,\"data\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"6.5.0\",\"snapshot\":false,\"status\":\"green\"}}}")
                                .withStatusCode(200)
                );
		return context;
    }

    @Test
    public void testQueriesForKibana() throws DiagnosticException {

		DiagnosticContext context = initializeKibana("6.5.0");
    	int totalRetries = new RunKibanaQueries().runBasicQueries(httpRestClient, context);

    	String[] pathnames;
    	pathnames = tempDir.list();
    	String[] kList = new String[]{"kibana_stats.json", "kibana_settings.json"};
    	List<String> list = Arrays.asList(kList);

        for (String pathname : pathnames) {
        	// if this Unit test fail, you can use this two logger to see the error file name
            // logger.info(Constants.CONSOLE, "pathnames.");
            // logger.info(Constants.CONSOLE, pathname);
        	assertTrue(list.contains(pathname));
        }
        assertEquals(list.size(), 2);

        JsonNode nodeData = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_stats.json");
        String pid = nodeData.path("process").path("pid").asText();
        String os = nodeData.path("os").path("platform").asText();
        String osParsed = SystemUtils.parseOperatingSystemName(nodeData.path("os").path("platform").asText());
   		assertEquals(pid, "32");
   		assertEquals(os, "linux");
   		assertEquals(osParsed, "linuxOS");

   		JsonNode nodeDataSettings = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_settings.json");
   		String uuid = nodeDataSettings.path("cluster_uuid").asText();
   		String version = nodeDataSettings.path("settings").path("kibana").path("version").asText();
   		assertEquals(uuid, "RLtzkhfBRUadN4WZ8fnnog");
   		assertEquals(version, "6.5.0");
    }

    @Test
	public void testSixversion() throws DiagnosticException {

		DiagnosticContext context = initializeKibana("6.5.0");
    	int totalRetries = new RunKibanaQueries().runBasicQueries(httpRestClient, context);

    	String[] pathnames;
    	pathnames = tempDir.list();
    	String[] kList = new String[]{"kibana_stats.json", "kibana_settings.json"};
    	List<String> list = Arrays.asList(kList);

        // For each pathname in the pathnames array
        for (String pathname : pathnames) {
        	assertTrue(list.contains(pathname));
        }
        assertEquals(list.size(), 2);

	}

	@Test
    public void testQueriesForKibana711() throws DiagnosticException {

		DiagnosticContext context = initializeKibana("7.11.0");
    	int totalRetries = new RunKibanaQueries().runBasicQueries(httpRestClient, context);

    	String[] pathnames;
    	pathnames = tempDir.list();
    	String[] kList = new String[]{"kibana_stats.json", 
    									"kibana_settings.json", 
    									"kibana_detection_engine_signals.json", 
    									"kibana_actions.json",  
    									"kibana_detection_engine_privileges.json", 
    									"kibana_task_manager_help.json", 
    									"kibana_detection_engine_find_1.json", 
    									"kibana_detection_engine_find_2.json", 
    									"kibana_alerts_1.json", 
    									"kibana_alerts_2.json"};
    	List<String> list = Arrays.asList(kList);

        for (String pathname : pathnames) {
        	// if this Unit test fail, you can use this two logger to see the error file name
            logger.info(Constants.CONSOLE, "pathnames.");
            logger.info(Constants.CONSOLE, pathname);
        	assertTrue(list.contains(pathname));
        }
        assertEquals(list.size(), 10);

        JsonNode nodeData = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_alerts_1.json");
        String page1 = nodeData.path("page").asText();
        String total1 = nodeData.path("total").asText();
        assertEquals(page1, "1");
   		assertEquals(total1, "4");

   		JsonNode nodeData1 = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_alerts_2.json");
        String page2 = nodeData1.path("page").asText();
        String total2 = nodeData1.path("total").asText();
        assertEquals(page2, "2");
   		assertEquals(total2, "4");

   		JsonNode nodeData2 = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_detection_engine_find_1.json");
        String page3 = nodeData2.path("page").asText();
        String total3 = nodeData2.path("total").asText();
        assertEquals(page3, "1");
   		assertEquals(total3, "4");

   		JsonNode nodeData3 = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_detection_engine_find_2.json");
        String page4 = nodeData3.path("page").asText();
        String total4 = nodeData3.path("total").asText();
        assertEquals(page4, "2");
   		assertEquals(total4, "4");

   		JsonNode nodeData5 = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_stats.json");
        String pid = nodeData5.path("process").path("pid").asText();
        String os = nodeData5.path("os").path("platform").asText();
        String osParsed = SystemUtils.parseOperatingSystemName(nodeData5.path("os").path("platform").asText());
   		assertEquals(pid, "32");
   		assertEquals(os, "linux");
   		assertEquals(osParsed, "linuxOS");

   		JsonNode nodeData6 = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_settings.json");
        String clusterUuid = nodeData6.path("cluster_uuid").asText();
        assertEquals(clusterUuid, "RLtzkhfBRUadN4WZ8fnnog");

   		JsonNode nodeData7 = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_task_manager_help.json");
        String username = nodeData7.path("username").asText();
        assertEquals(username, "elastic");
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

      mockServer
            .when(
                    request()
                            .withMethod("GET")
                            .withPath("/api/actions")
            )
            .respond(
                    response()
                            .withBody("[{\"id\":\"eec7ee50-7129-1111-9b41-17a1879ebc72\",\"actionTypeId\":\".webhook\",\"name\":\"webhook-auto-create-case\",\"config\":{\"method\":\"post\",\"hasAuth\":true,\"url\":\"https://7067a1cd7a8142a79ab8dec9304a5436.europe-west1.gcp.cloud.es.io/api/cases\",\"headers\":{\"Authorization\":\"Basic 1111\",\"customHeader\":\"CustomValue\",\"kbn-xsrf\":\"kibana\",\"Content-Type\":\"application/json\"}},\"isPreconfigured\":false,\"referencedByCount\":9}]")
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

      mockServer
            .when(
                    request()
                            .withMethod("GET")
                            .withPath("/api/actions")
            )
            .respond(
                    response()
                            .withBody("[{\"id\":\"eec7ee50-7129-3333-9b41-17a1879ebc72\",\"actionTypeId\":\".webhook\",\"name\":\"webhook-auto-create-case\",\"test\":{\"method\":\"post\",\"hasAuth\":true,\"url\":\"https://7067a1cd7a8142a79ab8dec9304a5436.europe-west1.gcp.cloud.es.io/api/cases\",\"headers\":{\"Authorization\":\"Basic 1111\",\"customHeader\":\"CustomValue\",\"kbn-xsrf\":\"kibana\",\"Content-Type\":\"application/json\"}},\"isPreconfigured\":false,\"referencedByCount\":9}]")
        );
        DiagnosticContext context = initializeKibana("7.10.0");
        int totalRetries = new RunKibanaQueries().runBasicQueries(httpRestClient, context);
        try {
            new RunKibanaQueries().filterActionsHeaders(context);
        } catch (RuntimeException e) {
            assertTrue(false);
        }
        JsonNode nodeData = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_actions.json");
        JsonNode config = nodeData.get(0).get("config");
        assertEquals(nodeData.get(0).path("id").asText(), "eec7ee50-7129-3333-9b41-17a1879ebc72");
        assertTrue((config == null || config.isNull()));

    }
}
