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
import com.elastic.support.diagnostics.commands.KibanaGetDetails;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestKibanaGetDetails {


    @Test
    public void testGetNodeNetworkAndLogInfo() {

        String kibana_node_stats = "{\"process\":{\"memory\":{\"heap\":{\"total_bytes\":470466560,\"used_bytes\":343398224,\"size_limit\":1740165498},\"resident_set_size_bytes\":587878400},\"pid\":32,\"event_loop_delay\":0.280181884765625,\"uptime_ms\":97230924},\"os\":{\"platform\":\"win32\",\"platform_release\":\"linux-4.15.0-1032-gcp\",\"load\":{\"1m\":1.37451171875,\"5m\":1.43408203125,\"15m\":1.34375},\"memory\":{\"total_bytes\":147879931904,\"free_bytes\":45620334592,\"used_bytes\":102259597312},\"uptime_ms\":18093713000,\"distro\":\"Centos\",\"distro_release\":\"Centos-7.8.2003\"},\"requests\":{\"disconnects\":0,\"total\":1,\"status_codes\":{\"302\":1}},\"concurrent_connections\":8,\"timestamp\":\"2021-01-06T01:35:11.324Z\",\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"7.9.0\",\"snapshot\":false,\"status\":\"green\"},\"last_updated\":\"2021-01-06T01:35:15.911Z\",\"collection_interval_ms\":5000,\"cluster_uuid\":\"RfBRUssssadN4WZssnnog\"}";
        JsonNode infoNodes = JsonYamlUtils.createJsonNodeFromString(kibana_node_stats);
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
}
