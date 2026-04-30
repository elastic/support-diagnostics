/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.rest;

import co.elastic.support.diagnostics.commands.RunClusterQueries;
import co.elastic.support.util.SystemProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRestExecCalls {
    private WireMockServer wireMockServer;
    private RestClient httpRestClient, httpsRestClient;
    private String temp = SystemProperties.userDir + SystemProperties.fileSeparator + "temp";
    private File tempDir = new File(temp);
    private String authStringEnc = new String(Base64.encodeBase64("elastic:elastic".getBytes()));

    @BeforeAll
    public void globalSetup() {
        wireMockServer = new WireMockServer(wireMockConfig().port(9880).httpsPort(9443));
        wireMockServer.start();
    }

    @AfterAll
    public void globalTeardown() {
        wireMockServer.stop();
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
            3000
        );
        httpsRestClient = RestClient.getClient(
            "localhost",
            9443,
            "https",
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
            3000
        );

        tempDir.mkdir();
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.resetAll();
        FileUtils.deleteQuietly(tempDir);
    }

    @Test
    public void testSimpleQuery() {
        wireMockServer.stubFor(get(urlEqualTo("/")).withHeader("Authorization", equalTo("Basic " + authStringEnc))
            .willReturn(aResponse().withBody("some_response_body")));

        RestResult result = httpRestClient.execQuery("/");
        assertEquals(200, result.getStatus());
        assertEquals("some_response_body", result.toString());
    }

    @Test
    public void testSecuredQuery() {
        String url = "/";

        wireMockServer.stubFor(get(urlEqualTo("/")).withHeader("Authorization", equalTo("Basic " + authStringEnc))
            .willReturn(aResponse().withBody("some_response_body")));

        RestResult result = httpRestClient.execQuery(url);
        assertEquals(200, result.getStatus());
        assertEquals("some_response_body", result.toString());
    }

    @Test
    public void testHttpsQuery() {
        String url = "/_nodes";
        wireMockServer.stubFor(get(urlEqualTo("/_nodes")).withHeader("Authorization", equalTo("Basic " + authStringEnc))
            .willReturn(aResponse().withBody("some_response_body")));

        RestResult result = httpsRestClient.execQuery(url);
        assertEquals(200, result.getStatus());
        assertEquals("some_response_body", result.toString());
    }

    @Test
    public void testFailThenSucceed() {
        RunClusterQueries cmd = new RunClusterQueries();
        List<RestEntry> entries = new ArrayList<>();
        entries.add(new RestEntry("nodes", "", ".json", true, "/_nodes", false));

        wireMockServer.stubFor(get(urlEqualTo("/_nodes")).inScenario("retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withBody("error_response_body").withStatus(502))
            .willSetStateTo("failed-once"));

        wireMockServer.stubFor(get(urlEqualTo("/_nodes")).inScenario("retry")
            .whenScenarioStateIs("failed-once")
            .willReturn(aResponse().withBody("error_response_body").withStatus(502))
            .willSetStateTo("failed-twice"));

        wireMockServer.stubFor(get(urlEqualTo("/_nodes")).inScenario("retry")
            .whenScenarioStateIs("failed-twice")
            .willReturn(aResponse().withBody("node_response_body").withStatus(200)));

        String targetFilename = temp + SystemProperties.fileSeparator + "nodes.json";
        int totalRetries = cmd.runQueries(httpRestClient, entries, temp, 3, 500);
        assertEquals(2, totalRetries);
        assertTrue(fileExistsWithText(targetFilename, "node_response_body"));
    }

    @Test
    public void testRetryAllFail() {
        RunClusterQueries cmd = new RunClusterQueries();
        List<RestEntry> entries = new ArrayList<>();
        entries.add(new RestEntry("nodes", "", ".json", true, "/_nodes", false));
        wireMockServer.stubFor(get(urlEqualTo("/_nodes")).willReturn(aResponse().withBody("error_response_body").withStatus(502)));

        String targetFilename = temp + SystemProperties.fileSeparator + "nodes.json";

        int totalRetries = cmd.runQueries(httpRestClient, entries, temp, 3, 500);
        assertEquals(4, totalRetries);
        assertTrue(fileExistsWithText(targetFilename, "error_response_body"));
    }

    @Test
    public void testAuthFailure() {
        RunClusterQueries cmd = new RunClusterQueries();
        List<RestEntry> entries = new ArrayList<>();
        entries.add(new RestEntry("nodes", "", ".json", true, "/_nodes", false));

        wireMockServer.stubFor(get(urlEqualTo("/_nodes")).willReturn(aResponse().withBody("autherror_response_body").withStatus(401)));

        int totalRetries = cmd.runQueries(httpRestClient, entries, temp, 3, 500);
        assertEquals(0, totalRetries);

        String targetFilename = temp + SystemProperties.fileSeparator + "nodes.json";
        assertTrue(fileExistsWithText(targetFilename, "autherror_response_body"));
    }

    @Test
    public void testConnectionRefusedPreservesCause() {
        RestClient unreachableClient = RestClient.getClient(
            "localhost",
            19999,
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
            1000,
            1000,
            1000);

        RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> unreachableClient.execGet("/"));
        assertNotNull(ex.getCause(), "RuntimeException must preserve original cause");
    }

    private boolean fileExistsWithText(String filename, String compare) {
        try {
            String fileContents = FileUtils.readFileToString(new File(filename), "UTF8");
            if (!fileContents.contains(compare)) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }
}
