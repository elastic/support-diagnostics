/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.diagnostics.DiagnosticException;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import co.elastic.support.rest.RestClient;
import com.vdurmont.semver4j.Semver;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestCheckKibanaVersion {

    private static final Logger logger = LogManager.getLogger(RestClient.class);
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

    private void initializeKibanaSettings(String version) {

        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/settings")
                )
                .respond(
                        response()
                                .withBody("{\"cluster_uuid\":\"RLtzkhfBRUadN4WZ8fnnog\",\"settings\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"" + version + "\",\"snapshot\":false,\"status\":\"green\"}}}")
                                .withStatusCode(200)
                );


    }

    private void initializeKibanaStats(String version) {

        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/stats")
                )
                .respond(
                        response()
                                .withBody("{\"kibana\":{\"uuid\":\"669ae985-31f7-493b-9910-522cac4d5479\",\"name\":\"6f5485cce678\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18117\",\"version\":\"" + version + "\",\"snapshot\":false,\"status\":\"green\"}}")
                                .withStatusCode(200)
                );


    }

    @Test
    public void testQueriesForKibanaWhenSettings() throws DiagnosticException {
		initializeKibanaSettings("6.5.0");
        Semver version = new CheckKibanaVersion().getKibanaVersion(httpRestClient);
   		assertEquals("6.5.0", version.getValue());
    }

    @Test
    public void testQueriesForKibanaWhenStats() throws DiagnosticException {
        initializeKibanaStats("8.0.0-rc2");
        Semver version = new CheckKibanaVersion().getKibanaVersion(httpRestClient);
        assertEquals("8.0.0-rc2", version.getValue());
    }

    @Test
    public void testQueriesForKibanaWhenStatsAndSettings() throws DiagnosticException {
        initializeKibanaStats("8.0.0-rc2");
        initializeKibanaSettings("7.17.0");
        Semver version = new CheckKibanaVersion().getKibanaVersion(httpRestClient);
        // the version should be computed from the /api/stats call
        assertEquals("8.0.0-rc2", version.getValue());
    }

    /**
     * version is Mandatory as we use the version to define the APIs that will be executed
     */
    @Test
    public void testQueriesForKibanaEmptyVersion() {
        // the body is different here than in the previous test
        // HERE we have removed the version from the JSON response to trigger the expection
        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/settings")
                )
                .respond(
                        response()
                                .withBody("{\"cluster_uuid\":\"RLtzkhfBRUadN4WZ8fnnog\",\"settings\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"snapshot\":false,\"status\":\"green\"}}}")
                                .withStatusCode(200)
                );

        try {
            Semver version = new CheckKibanaVersion().getKibanaVersion(httpRestClient);
            // if they are more than one node in Kibana we need to throw an Exception
            assertTrue(false);
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
        // the body is different here than in the previous test
        // HERE we have removed the version from the JSON response to trigger the expection
        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/settings")
                )
                .respond(
                        response()
                                .withBody("{\"cluster_uuid\":\"RLtzkhfBRUadN4WZ8fnnog\",\"settings\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"a.v.c\",\"snapshot\":false,\"status\":\"green\"}}}")
                                .withStatusCode(200)
                );

        try {
            Semver version = new CheckKibanaVersion().getKibanaVersion(httpRestClient);
            // if they are more than one node in Kibana we need to throw an Exception
            assertTrue(false);
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
        // the body is different here than in the previous test
        // HERE we have removed the version from the JSON response to trigger the expection
        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/settings")
                )
                .respond(
                        response()
                                .withBody("{\"cluster_uuid\":\"RLtzkhfBRUadN4WZ8fnnog\",\"settings\":{\"xpack\":{\"default_admin_email\":null},\"kibana\":{\"uuid\":\"a4f369ef-fecd-46b7-8b16-c6c3f885d9ec\",\"name\":\"13d5e793ea51\",\"index\":\".kibana\",\"host\":\"0.0.0.0\",\"port\":18648,\"locale\":\"en\",\"transport_address\":\"0.0.0.0:18648\",\"version\":\"test-6.5.1\",\"snapshot\":false,\"status\":\"green\"}}}")
                                .withStatusCode(200)
                );

        try {
            Semver version = new CheckKibanaVersion().getKibanaVersion(httpRestClient);
            // if they are more than one node in Kibana we need to throw an Exception
            assertTrue(false);
        } catch (DiagnosticException e) {
            assertEquals(e.getMessage(), "Kibana version format is wrong - unable to continue. (test-6.5.1)");
        }
    }
}
