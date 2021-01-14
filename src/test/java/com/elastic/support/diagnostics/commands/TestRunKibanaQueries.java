package com.elastic.support.diagnostics;

import com.elastic.support.Constants;
import com.elastic.support.util.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import com.elastic.support.diagnostics.commands.RunKibanaQueries;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import org.apache.http.client.config.RequestConfig;
import com.elastic.support.rest.RestEntryConfig;
import org.mockserver.integration.ClientAndServer;
import com.elastic.support.util.ResourceCache;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.Parameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestEntry;
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

    private DiagnosticContext initializeKibana(String version) {

    	DiagnosticContext context = new DiagnosticContext();
    	RestEntryConfig builder = new RestEntryConfig(version);

        Map restCalls = JsonYamlUtils.readYamlFromClasspath(Constants.KIBANA_REST, true);
        Map<String, RestEntry> entries = builder.buildEntryMap(restCalls);
    	context.elasticRestCalls = entries;

    	context.tempDir = tempDir.getPath();
    	
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
		                        .withQueryStringParameters(
		                                new Parameter("extended", "true")
		                        )
		        )
		        .respond(
		                response()
		                        .withBody("{\"process\":{\"memory\":{\"heap\":{\"total_bytes\":470466560,\"used_bytes\":343398224,\"size_limit\":1740165498},\"resident_set_size_bytes\":587878400},\"pid\":32,\"event_loop_delay\":0.280181884765625,\"uptime_ms\":97230924},\"os\":{\"platform\":\"linux\",\"platform_release\":\"linux-4.15.0-1032-gcp\",\"load\":{\"1m\":1.37451171875,\"5m\":1.43408203125,\"15m\":1.34375},\"memory\":{\"total_bytes\":147879931904,\"free_bytes\":45620334592,\"used_bytes\":102259597312},\"uptime_ms\":18093713000,\"distro\":\"Centos\",\"distro_release\":\"Centos-7.8.2003\"},\"requests\":{\"disconnects\":0,\"total\":1,\"status_codes\":{\"302\":1}},\"concurrent_connections\":8,\"timestamp\":\"2021-01-06T01:35:11.324Z\",\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"7.9.0\",\"snapshot\":false,\"status\":\"green\"},\"last_updated\":\"2021-01-06T01:35:15.911Z\",\"collection_interval_ms\":5000,\"cluster_uuid\":\"RfBRUssssadN4WZssnnog\"}")
		        );

		return context;
    }

    @Test
    public void testQueriesForKibana() {

		DiagnosticContext context = initializeKibana("6.5.0");
    	int totalRetries = new RunKibanaQueries().runBasicQueries(httpRestClient, context);

    	String[] pathnames;
    	pathnames = tempDir.list();
    	String[] kList = new String[]{"kibana_node_stats.json", "kibana_settings.json"};
    	List<String> list = Arrays.asList(kList);

        // For each pathname in the pathnames array
        for (String pathname : pathnames) {
        	assertTrue(list.contains(pathname));
        }
        assertEquals(list.size(), 2);

        JsonNode nodeData = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "kibana_node_stats.json");
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
	public void testExecSystemLocalCommands() {

		DiagnosticContext context = initializeKibana("6.5.0");
    	int totalRetries = new RunKibanaQueries().runBasicQueries(httpRestClient, context);

    	String[] pathnames;
    	pathnames = tempDir.list();
    	String[] kList = new String[]{"kibana_node_stats.json", "kibana_settings.json"};
    	List<String> list = Arrays.asList(kList);

        // For each pathname in the pathnames array
        for (String pathname : pathnames) {
        	assertTrue(list.contains(pathname));
        }
        assertEquals(list.size(), 2);

		context.diagnosticInputs = new DiagnosticInputs();
		context.diagnosticInputs.diagType = Constants.kibanaLocal;
		context.tempDir = tempDir.getPath();
		context.dockerPresent = false;
		SystemCommand syscmd = null;
		syscmd = new RunKibanaQueries().execSystemCommands(context);
		assertTrue(syscmd instanceof LocalSystem);

	}

}
