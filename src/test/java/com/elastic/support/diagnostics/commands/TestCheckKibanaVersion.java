package com.elastic.support.diagnostics;

import com.elastic.support.Constants;
import com.elastic.support.util.*;
import org.junit.jupiter.api.Test;
import com.elastic.support.diagnostics.commands.CheckKibanaVersion;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.Parameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestEntry;
import com.vdurmont.semver4j.Semver;

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
           3000,
           3000,
           3000);
    }

    @AfterEach
    public void tearDown() {
        mockServer.reset();
    }

    private void initializeKibana() {

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


    }

    @Test
    public void testQueriesForKibana() {
		initializeKibana();
        Semver version = new CheckKibanaVersion().getKibanaVersion(httpRestClient);
        // the version 6.5.0 was defined on the json object on the mockServer boby.
   		assertEquals("6.5.0", version.getValue());
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
        } catch (RuntimeException e) {
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
        } catch (RuntimeException e) {
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
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "Kibana version format is wrong - unable to continue. (test-6.5.1)");
        }
    }
}
