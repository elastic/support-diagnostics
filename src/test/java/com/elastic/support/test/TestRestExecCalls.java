package com.elastic.support.test;

import com.elastic.support.diagnostics.DiagConfig;
import com.elastic.support.diagnostics.commands.RunClusterQueriesCmd;
import com.elastic.support.rest.RestCallManifest;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestClientBuilder;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.SystemProperties;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRestExecCalls {

    private final Logger logger = LoggerFactory.getLogger(TestRestExecCalls.class);
    private ClientAndServer mockServer;
    private Map config;
    private PoolingHttpClientConnectionManager connectionManager;
    private RestClientBuilder builder = new RestClientBuilder();
    private RestClient httpRestClient, httpsRestClient;
    private String temp = SystemProperties.userDir + SystemProperties.fileSeparator + "temp";
    private File tempDir = new File(temp);
    private String authStringEnc = new String(Base64.encodeBase64("elastic:elastic".getBytes()));

    @BeforeAll
    public void globalSetup() {

        mockServer = startClientAndServer(9200);

    }

    @AfterAll
    public void globalTeardoown() {

        mockServer.stop();
    }

    @BeforeEach
    public void setup() {

        httpRestClient = builder
                .setPooledConnections(true)
                .setSocketTimeout(30000)
                .setConnectTimeout(30000)
                .setRequestTimeout(30000)
                .setHost("localhost")
                .setPort(9200)
                .setScheme("http")
                .setUser("elastic")
                .setPassword("elastic")
                .build();

        httpsRestClient = builder
                .setPooledConnections(true)
                .setSocketTimeout(30000)
                .setConnectTimeout(30000)
                .setRequestTimeout(30000)
                .setHost("localhost")
                .setPort(9200)
                .setScheme("https")
                .setUser("elastic")
                .setPassword("elastic")
                .build();

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
    public void testMultipleFailures() {

        RunClusterQueriesCmd cmd = new RunClusterQueriesCmd();

        DiagConfig diagConfig = new DiagConfig();

        diagConfig.setTextFileExtensions(new ArrayList<>());
        diagConfig.setCallRetries(3);
        diagConfig.setPauseRetries(5000);
        List<String> requireRetry = new ArrayList<>();
        requireRetry.add("nodes");
        diagConfig.setRequireRetry(requireRetry);

        Map<String, String> calls = new LinkedHashMap<>();
        calls.put("nodes", "/_nodes?pretty");
        calls.put("version", "/");
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
                                .withBody("error_response_body")
                                .withStatusCode(502)
                );

        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/"),
                        Times.exactly(1)
                )
                .respond(
                        response()
                                .withBody("version_response_body")
                                .withStatusCode(200)
                );
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

        String targetFilename = temp + SystemProperties.fileSeparator + "nodes.json";
        logger.error(targetFilename);

        RestCallManifest restCallManifest = cmd.runQueries(httpRestClient, calls, temp, diagConfig);
        assertEquals(3, restCallManifest.getAttempts("nodes"));
        assertFalse(restCallManifest.getSuccess("nodes"));
        assertEquals(1, restCallManifest.getAttempts("version"));
        assertTrue(restCallManifest.getSuccess("version"));
        assertTrue( fileExistsWithText(targetFilename, "error_response_body"));

    }

    @Test
    public void testFailureThenSuccess() {

        RunClusterQueriesCmd cmd = new RunClusterQueriesCmd();
        DiagConfig diagConfig = new DiagConfig();

        diagConfig.setTextFileExtensions(new ArrayList<>());
        diagConfig.setCallRetries(3);
        diagConfig.setPauseRetries(5000);
        List<String> requireRetry = new ArrayList<>();
        requireRetry.add("nodes");
        diagConfig.setRequireRetry(requireRetry);

        Map<String, String> calls = new LinkedHashMap<>();
        calls.put("nodes", "/_nodes?pretty");
        calls.put("version", "/");

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
                                .withBody("error_response_body")
                                .withStatusCode(502)
                );

        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/"),
                        Times.exactly(1)
                )
                .respond(
                        response()
                                .withBody("version_response_body")
                                .withStatusCode(200)
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
        logger.error(targetFilename);

        RestCallManifest restCallManifest = cmd.runQueries(httpRestClient, calls, temp, diagConfig);
        assertEquals(3, restCallManifest.getAttempts("nodes"));
        assertTrue(restCallManifest.getSuccess("nodes"));
        assertEquals(1, restCallManifest.getAttempts("version"));
        assertTrue(restCallManifest.getSuccess("version"));
        assertTrue( fileExistsWithText(targetFilename, "node_response_body"));

    }

    @Test
    public void testAuthFailure() {

        RunClusterQueriesCmd cmd = new RunClusterQueriesCmd();

        DiagConfig diagConfig = new DiagConfig();

        diagConfig.setTextFileExtensions(new ArrayList<>());
        diagConfig.setCallRetries(3);
        diagConfig.setPauseRetries(5000);
        List<String> requireRetry = new ArrayList<>();
        requireRetry.add("nodes");
        diagConfig.setRequireRetry(requireRetry);

        Map<String, String> calls = new LinkedHashMap<>();
        calls.put("nodes", "/_nodes?pretty");
        calls.put("version", "/");

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

        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/"),
                        Times.exactly(1)
                )
                .respond(
                        response()
                                .withBody("autherror_response_body")
                                .withStatusCode(401)
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
        logger.error(targetFilename);

        RestCallManifest restCallManifest = cmd.runQueries(httpsRestClient, calls, temp, diagConfig);
        assertEquals(1, restCallManifest.getAttempts("nodes"));
        assertFalse(restCallManifest.getSuccess("nodes"));
        assertEquals(1, restCallManifest.getAttempts("version"));
        assertFalse(restCallManifest.getSuccess("version"));
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
