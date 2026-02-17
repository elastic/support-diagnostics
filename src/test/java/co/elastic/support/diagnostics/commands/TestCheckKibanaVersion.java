/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.rest.RestClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.semver4j.Semver;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestCheckKibanaVersion {
    private WireMockServer wireMockServer;
    private RestClient httpRestClient;

    @BeforeAll
    public void globalSetup() {
        wireMockServer = new WireMockServer(wireMockConfig().port(9880));
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
            Map.of(),
            3000,
            3000,
            3000
        );
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.resetAll();
    }

    private void initializeKibanaSettings(String version) {

        wireMockServer.stubFor(get(urlEqualTo("/api/settings")).willReturn(aResponse().withBody(
            "{\"cluster_uuid\":\"RLtzkhfBRUadN4WZ8fnnog\",\"settings\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\""
                + version + "\",\"snapshot\":false,\"status\":\"green\"}}}").withStatus(200)));
    }

    private void initializeKibanaStats(String version) {

        wireMockServer.stubFor(get(urlEqualTo("/api/stats")).willReturn(aResponse().withBody(
            "{\"kibana\":{\"uuid\":\"669ae985-31f7-493b-9910-522cac4d5479\",\"name\":\"6f5485cce678\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18117\",\"version\":\""
                + version + "\",\"snapshot\":false,\"status\":\"green\"}}").withStatus(200)));
    }

    @Test
    public void testQueriesForKibanaWhenStats() throws DiagnosticException {
        initializeKibanaStats("8.1.2");
        Semver version = new CheckKibanaVersion().getKibanaVersion(httpRestClient);
        assertEquals("8.1.2", version.getVersion());
    }

    @Test
    public void testQueriesForKibanaWhenStatsWithRC() throws DiagnosticException {
        initializeKibanaStats("9.0.0-beta1");
        Semver version = new CheckKibanaVersion().getKibanaVersion(httpRestClient);
        assertEquals("9.0.0", version.getVersion());
    }

    @Test
    public void testQueriesForKibanaWhenStatsAndSettings() throws DiagnosticException {
        initializeKibanaStats("6.5.0");
        initializeKibanaSettings("6.5.0");
        Semver version = new CheckKibanaVersion().getKibanaVersion(httpRestClient);
        assertEquals("6.5.0", version.getVersion());
    }

    /**
     * version is Mandatory as we use the version to define the APIs that will be executed
     */
    @Test
    public void testQueriesForKibanaEmptyVersion() {
        // The response body contains no version
        wireMockServer.stubFor(get(urlEqualTo("/api/stats")).willReturn(aResponse().withBody("{\"kibana\": {}}").withStatus(200)));

        try {
            new CheckKibanaVersion().getKibanaVersion(httpRestClient);
            fail("Expected to fail");
        } catch (DiagnosticException e) {
            assertEquals(e.getMessage(), "Kibana version format is wrong - unable to continue. ()");
        }
    }

    /**
     * version is Mandatory as we use the version to define the APIs that will be executed
     * The format of our version is stable and numeric
     */
    @Test
    public void testQueriesForKibanaCorruptedVersion() {
        initializeKibanaStats("a.v.c");
        try {
            new CheckKibanaVersion().getKibanaVersion(httpRestClient);
            // if they are more than one node in Kibana we need to throw an Exception
            fail("Expected to fail");
        } catch (DiagnosticException e) {
            assertEquals(e.getMessage(), "Kibana version format is wrong - unable to continue. (a.v.c)");
        }
    }

    /**
     * version is Mandatory as we use the version to define the APIs that will be executed
     * The format of our version is stable and numeric
     */
    @Test
    public void testQueriesForKibanaTextWithVersion() {
        initializeKibanaStats("test-6.5.1");

        try {
            new CheckKibanaVersion().getKibanaVersion(httpRestClient);
            // if they are more than one node in Kibana we need to throw an Exception
            fail("Expected to fail");
        } catch (DiagnosticException e) {
            assertEquals(e.getMessage(), "Kibana version format is wrong - unable to continue. (test-6.5.1)");
        }
    }
}
