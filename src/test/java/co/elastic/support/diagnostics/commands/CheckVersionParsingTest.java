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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.semver4j.Semver;

import static co.elastic.support.testutil.ContainerTestHelper.loadDiagConfig;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckVersionParsingTest {

    private WireMockServer wireMockServer;

    @BeforeAll
    void startMockServer() {
        wireMockServer = new WireMockServer(wireMockConfig().bindAddress("127.0.0.1").dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    void stopMockServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    void resetStubs() {
        wireMockServer.resetAll();
    }

    private RestClient clientForMock() {
        var config = loadDiagConfig();
        return RestClient.getClient(
                "127.0.0.1", wireMockServer.port(), "http",
                "", "", "", 8080, "", "", "", "",
                false, config.extraHeaders,
                config.connectionTimeout, config.connectionRequestTimeout, config.socketTimeout);
    }

    @Test
    void elasticsearch_parsesSemanticVersion() throws DiagnosticException {
        wireMockServer.stubFor(any(anyUrl())
                .willReturn(aResponse().withBody("{\"version\":{\"number\":\"9.3.0\"}}")));
        try (RestClient client = clientForMock()) {
            Semver version = CheckElasticsearchVersion.getElasticsearchVersion(client);
            assertEquals(9, version.getMajor());
            assertEquals(3, version.getMinor());
            assertEquals(0, version.getPatch());
        }
    }

    @Test
    void elasticsearch_preReleaseVersionStripped() throws DiagnosticException {
        wireMockServer.stubFor(any(anyUrl())
                .willReturn(aResponse().withBody("{\"version\":{\"number\":\"9.3.0-SNAPSHOT\"}}")));
        try (RestClient client = clientForMock()) {
            Semver version = CheckElasticsearchVersion.getElasticsearchVersion(client);
            // withClearedPreRelease() removes the -SNAPSHOT suffix
            assertTrue(version.getPreRelease().isEmpty(),
                    "Pre-release should be cleared, got: " + version);
        }
    }

    @Test
    void elasticsearch_invalidResponse_throwsDiagnosticException() {
        wireMockServer.stubFor(any(anyUrl())
                .willReturn(aResponse().withStatus(503).withBody("unavailable")));
        try (RestClient client = clientForMock()) {
            assertThrows(DiagnosticException.class,
                    () -> CheckElasticsearchVersion.getElasticsearchVersion(client));
        }
    }

    @Test
    void kibana_parsesSemanticVersion() throws DiagnosticException {
        wireMockServer.stubFor(any(urlEqualTo("/api/stats"))
                .willReturn(aResponse().withBody("{\"kibana\":{\"version\":\"9.3.0\"}}")));
        try (RestClient client = clientForMock()) {
            Semver version = CheckKibanaVersion.getKibanaVersion(client);
            assertEquals(9, version.getMajor());
            assertEquals(3, version.getMinor());
        }
    }

    @Test
    void kibana_invalidVersion_throwsDiagnosticException() {
        wireMockServer.stubFor(any(anyUrl())
                .willReturn(aResponse().withBody("{\"kibana\":{\"version\":\"not-semver\"}}")));
        try (RestClient client = clientForMock()) {
            assertThrows(DiagnosticException.class,
                    () -> CheckKibanaVersion.getKibanaVersion(client));
        }
    }

    @Test
    void logstash_parsesSemanticVersion() throws DiagnosticException {
        wireMockServer.stubFor(any(urlEqualTo("/"))
                .willReturn(aResponse().withBody("{\"version\":\"9.3.0\",\"host\":\"logstash\"}")));
        try (RestClient client = clientForMock()) {
            Semver version = CheckLogstashVersion.getLogstashVersion(client);
            assertEquals(9, version.getMajor());
            assertEquals(3, version.getMinor());
        }
    }

    @Test
    void logstash_preReleaseVersionStripped() throws DiagnosticException {
        wireMockServer.stubFor(any(anyUrl())
                .willReturn(aResponse().withBody("{\"version\":\"9.3.0-SNAPSHOT\"}")));
        try (RestClient client = clientForMock()) {
            Semver version = CheckLogstashVersion.getLogstashVersion(client);
            assertTrue(version.getPreRelease().isEmpty(),
                    "Pre-release should be cleared, got: " + version);
        }
    }
}
