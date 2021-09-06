/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.rest;

import co.elastic.support.diagnostics.commands.RunClusterQueries;
import co.elastic.support.util.SystemProperties;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.*;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.mockserver.configuration.ConfigurationProperties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRestExecCalls {

    private final Logger logger = LoggerFactory.getLogger(TestRestExecCalls.class);
    private ClientAndServer mockServer;
    private RestClient httpRestClient, httpsRestClient;
    private String temp = SystemProperties.userDir + SystemProperties.fileSeparator + "temp";
    private File tempDir = new File(temp);
    private String authStringEnc = new String(Base64.encodeBase64("elastic:elastic".getBytes()));

    @BeforeAll
    public void globalSetup() {
        mockServer = startClientAndServer(9880);
        // mockserver by default is in verboce mode (useful when creating new test), move it to warning.
        ConfigurationProperties.disableSystemOut(true);
        ConfigurationProperties.logLevel("WARN");

    }

    @AfterAll
    public void globalTeardown() {
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
        httpsRestClient = RestClient.getClient(
            "localhost", 
            9880, 
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
           3000);

        tempDir.mkdir();

    }

    @AfterEach
    public void tearDown() {

        mockServer.reset();
        FileUtils.deleteQuietly(tempDir);

    }

    @Test
    public void testSimpleQuery() {

        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/")
                                .withHeader(new Header("Authorization", "Basic " + authStringEnc))
                                .withQueryStringParameters(
                                        new Parameter("pretty")
                                )
                )
                .respond(
                        response()
                                .withBody("some_response_body")
                );


        RestResult result = httpRestClient.execQuery("/?pretty");
        assertEquals(200, result.getStatus());
        assertEquals("some_response_body", result.toString());
    }

    @Test
    public void testSecuredQuery() {

        String url = "/";

        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/")
                                .withHeader(new Header("Authorization", "Basic " + authStringEnc))
                )
                .respond(
                        response()
                                .withBody("some_response_body")
                );

        RestResult result = httpRestClient.execQuery(url);
        assertEquals(200, result.getStatus());
        assertEquals("some_response_body", result.toString());

    }

    @Test
    public void testHttpsQuery() {

        String url = "/_nodes?pretty";
        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/_nodes")
                                .withQueryStringParameter(new Parameter("pretty"))
                                .withSecure(true)
                                .withHeader(new Header("Authorization", "Basic " + authStringEnc))
                )
                .respond(
                        response()
                                .withBody("some_response_body")
                );

        RestResult result = httpsRestClient.execQuery(url);
        assertEquals(200, result.getStatus());
        assertEquals("some_response_body", result.toString());

    }

    @Test
    public void testFailThenSucceed() {

        RunClusterQueries cmd = new RunClusterQueries();
        List<RestEntry> entries = new ArrayList<>();
        entries.add(new RestEntry("nodes", "", ".json", true, "/_nodes?pretty", false));
        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/_nodes")
                                .withQueryStringParameter(new Parameter("pretty")),
                        Times.exactly(2)
                )
                .respond(
                        response()
                                .withBody("error_response_body")
                                .withStatusCode(502)
                );

        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/_nodes")
                                .withQueryStringParameter(new Parameter("pretty")),
                        Times.exactly(1)
                )
                .respond(
                        response()
                                .withBody("node_response_body")
                                .withStatusCode(200)
                );

        String targetFilename = temp + SystemProperties.fileSeparator + "nodes.json";
        int totalRetries = cmd.runQueries(httpRestClient, entries, temp, 3, 500);
        assertEquals(2, totalRetries);
        assertTrue( fileExistsWithText(targetFilename, "node_response_body"));


    }

    @Test
    public void testRetryAllFail() {

        RunClusterQueries cmd = new RunClusterQueries();
        List<RestEntry> entries = new ArrayList<>();
        entries.add(new RestEntry("nodes", "", ".json", true, "/_nodes?pretty", false));
        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/_nodes")
                                .withQueryStringParameter(new Parameter("pretty")),
                        Times.exactly(4)
                )
                .respond(
                        response()
                                .withBody("error_response_body")
                                .withStatusCode(502)
                );


        String targetFilename = temp + SystemProperties.fileSeparator + "nodes.json";

        int totalRetries = cmd.runQueries(httpRestClient, entries, temp, 3, 500);
        assertEquals(4, totalRetries);
        assertTrue( fileExistsWithText(targetFilename, "error_response_body"));

    }


    @Test
    public void testAuthFailure() {

        RunClusterQueries cmd = new RunClusterQueries();
        List<RestEntry> entries = new ArrayList<>();
        entries.add(new RestEntry("nodes", "", ".json", true, "/_nodes?pretty", false));

        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/_nodes")
                                .withQueryStringParameter(new Parameter("pretty")),
                        Times.exactly(1)
                )
                .respond(
                        response()
                                .withBody("autherror_response_body")
                                .withStatusCode(401)
                );

        int totalRetries = cmd.runQueries(httpRestClient, entries, temp, 3, 500);
        assertEquals(0, totalRetries);

        String targetFilename = temp + SystemProperties.fileSeparator + "nodes.json";
        assertTrue( fileExistsWithText(targetFilename, "autherror_response_body"));

    }


    private boolean fileExistsWithText(String filename, String compare){
        try {
            String fileContents = FileUtils.readFileToString(new File(filename), "UTF8");
            if (! fileContents.contains(compare) ){
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }


}
