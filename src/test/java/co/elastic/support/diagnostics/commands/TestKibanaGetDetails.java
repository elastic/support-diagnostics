/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.diagnostics.DiagConfig;
import co.elastic.support.diagnostics.DiagnosticInputs;
import co.elastic.support.util.JsonYamlUtils;
import co.elastic.support.util.ResourceCache;
import co.elastic.support.Constants;
import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.diagnostics.ProcessProfile;
import org.junit.jupiter.api.Test;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.rest.RestEntryConfig;
import org.mockserver.integration.ClientAndServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.*;
import co.elastic.support.rest.RestClient;
import co.elastic.support.rest.RestEntry;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestKibanaGetDetails {

    private ClientAndServer mockServer;
    private RestClient httpRestClient, httpsRestClient;

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
    }

    @AfterEach
    public void tearDown() {
        mockServer.reset();
    }

    @Test
    public void testGetNodeNetworkAndLogInfo() {

        String kibana_stats = "{\"process\":{\"memory\":{\"heap\":{\"total_bytes\":470466560,\"used_bytes\":343398224,\"size_limit\":1740165498},\"resident_set_size_bytes\":587878400},\"pid\":32,\"event_loop_delay\":0.280181884765625,\"uptime_ms\":97230924},\"os\":{\"platform\":\"win32\",\"platform_release\":\"linux-4.15.0-1032-gcp\",\"load\":{\"1m\":1.37451171875,\"5m\":1.43408203125,\"15m\":1.34375},\"memory\":{\"total_bytes\":147879931904,\"free_bytes\":45620334592,\"used_bytes\":102259597312},\"uptime_ms\":18093713000,\"distro\":\"Centos\",\"distro_release\":\"Centos-7.8.2003\"},\"requests\":{\"disconnects\":0,\"total\":1,\"status_codes\":{\"302\":1}},\"concurrent_connections\":8,\"timestamp\":\"2021-01-06T01:35:11.324Z\",\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"7.9.0\",\"snapshot\":false,\"status\":\"green\"},\"last_updated\":\"2021-01-06T01:35:15.911Z\",\"collection_interval_ms\":5000,\"cluster_uuid\":\"RfBRUssssadN4WZssnnog\"}";
        JsonNode infoNodes = JsonYamlUtils.createJsonNodeFromString(kibana_stats);
        KibanaGetDetails testClass = new KibanaGetDetails();
        List<ProcessProfile> nodeProfiles = new ArrayList<>();
        nodeProfiles = testClass.getNodeNetworkAndLogInfo(infoNodes);

        assertEquals(nodeProfiles.size(), 1);
        assertEquals(nodeProfiles.get(0).pid, "32");
        assertEquals(nodeProfiles.get(0).name, "13d5e793ea51");
        assertEquals(nodeProfiles.get(0).isDocker, false);
        assertEquals(nodeProfiles.get(0).os, Constants.winPlatform);
        assertEquals(nodeProfiles.get(0).networkHost, "0.0.0.0");
        assertEquals(nodeProfiles.get(0).httpPublishAddr, "0.0.0.0");
        assertEquals(nodeProfiles.get(0).httpPort, 18648);
    }


    @Test
    public void testFindTargetNode() {

        KibanaGetDetails testClass = new KibanaGetDetails();
        List<ProcessProfile> nodeProfiles = new ArrayList<>();
        ProcessProfile diagNode = new ProcessProfile();
        diagNode.name = "kibana-name";
        diagNode.pid = "1";
        diagNode.isDocker = true;
        nodeProfiles.add(diagNode);
        ProcessProfile testedNodeProfiles = testClass.findTargetNode(nodeProfiles);
        assertEquals(testedNodeProfiles.pid, "1");
        assertEquals(testedNodeProfiles.name, "kibana-name");
        assertEquals(testedNodeProfiles.isDocker, true);
    }

   /**
    * Kibana is working in single mode, so the target node will be the only host stored on the nodeProfiles List
    * if they are two nodes is an error.
    *
    */
    @Test
    public void testClusterFindTargetNode() {
        
        KibanaGetDetails testClass = new KibanaGetDetails();
        List<ProcessProfile> nodeProfiles = new ArrayList<>();
        ProcessProfile diagNode = new ProcessProfile();
        diagNode.name = "kibana-name";
        diagNode.pid = "1";
        diagNode.isDocker = true;
        nodeProfiles.add(diagNode);

        ProcessProfile diagNode2 = new ProcessProfile();
        diagNode2.name = "kibana-name";
        diagNode2.pid = "1";
        diagNode2.isDocker = true;
        nodeProfiles.add(diagNode2);

        try {
            ProcessProfile testedNodeProfiles = testClass.findTargetNode(nodeProfiles);
            // if they are more than one node in Kibana we need to throw an Exception
            assertTrue(false);
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "Unable to get Kibana process profile.");
        }
    }

    @Test
    public void testFunctionGetStats() throws DiagnosticException {

        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/stats")
                )
                .respond(
                        response()
                            .withBody("{\"process\":{\"memory\":{\"heap\":{\"total_bytes\":470466560,\"used_bytes\":343398224,\"size_limit\":1740165498},\"resident_set_size_bytes\":587878400},\"pid\":32,\"event_loop_delay\":0.280181884765625,\"uptime_ms\":97230924},\"os\":{\"platform\":\"linux\",\"platform_release\":\"linux-4.15.0-1032-gcp\",\"load\":{\"1m\":1.37451171875,\"5m\":1.43408203125,\"15m\":1.34375},\"memory\":{\"total_bytes\":147879931904,\"free_bytes\":45620334592,\"used_bytes\":102259597312},\"uptime_ms\":18093713000,\"distro\":\"Centos\",\"distro_release\":\"Centos-7.8.2003\"},\"requests\":{\"disconnects\":0,\"total\":1,\"status_codes\":{\"302\":1}},\"concurrent_connections\":8,\"timestamp\":\"2021-01-06T01:35:11.324Z\",\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"7.9.0\",\"snapshot\":false,\"status\":\"green\"},\"last_updated\":\"2021-01-06T01:35:15.911Z\",\"collection_interval_ms\":5000,\"cluster_uuid\":\"RfBRUssssadN4WZssnnog\"}")
                            .withStatusCode(401)
                );

        ResourceCache resourceCache = new ResourceCache();
        resourceCache.addRestClient(Constants.restInputHost, httpRestClient);
        Map diagMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
        DiagnosticContext context = new DiagnosticContext(new DiagConfig(diagMap), new DiagnosticInputs(), resourceCache, true);
        Map restCalls = JsonYamlUtils.readYamlFromClasspath(Constants.KIBANA_REST, true);
        RestEntryConfig builder = new RestEntryConfig("7.10.0");
        Map<String, RestEntry> entries = builder.buildEntryMap(restCalls);
        context.elasticRestCalls = entries;

        KibanaGetDetails testClass = new KibanaGetDetails();
        try {
            testClass.getStats(context);
        } catch (DiagnosticException e) {
            assertEquals(e.getMessage(), "Kibana responded with [401] for [/api/stats]. Unable to proceed.");
        } finally {
            resourceCache.close();
        }
    }
}
