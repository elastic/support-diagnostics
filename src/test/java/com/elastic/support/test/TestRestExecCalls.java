package com.elastic.support.test;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestClientBuilder;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.JsonYamlUtils;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.*;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.mockserver.MockServer;
import org.mockserver.model.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRestExecCalls {

    private final Logger logger = LoggerFactory.getLogger(TestRestExecCalls.class);
    private ClientAndServer mockServer;
    private Map config;
    private PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    private RestClient restClient;


    @BeforeAll
    public void globalSetup9() {
        mockServer = startClientAndServer(9200);
        config = JsonYamlUtils.readYamlFromClasspath("diags-test.yml", true);
        connectionManager.setMaxTotal(25);
        connectionManager.setDefaultMaxPerRoute(10);

        RestClientBuilder builder = new RestClientBuilder();
        restClient = builder
                .setClientTimeouts(30000,
                        30000,
                        30000)
                .setConnectionManager(connectionManager)
                .build();
        restClient.configureDestination("localhost", 9200, "http");
    }

    @AfterAll
    public void globalTeardoown(){
        mockServer.stop();
    }

    @BeforeEach
    public void resetExpectations(){
        mockServer.reset();
    }

    @Test
    public void testSimpleQuery() {
  /*      mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/")
                                .withQueryStringParameters(
                                        param("pretty", "true")
                                )
                )
                .respond(
                        response()
                                .withBody("some_response_body")
                );





        String result = restClient.execQuery("/?pretty=true").toString();
        logger.error(result);*/
    }

    @Test
    public void testAllFailures() {

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(25);
        connectionManager.setDefaultMaxPerRoute(10);

        RestClientBuilder builder = new RestClientBuilder();
        RestClient client = builder
                .setClientTimeouts(30000,
                        30000,
                        30000)
                .setConnectionManager(connectionManager)
                .build();
        client.configureDestination("localhost", 9200, "http");

        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/_nodes")
                                .withQueryStringParameter(new Parameter("pretty")),
                        Times.exactly(3)
                )
                .respond(
                        response()
                                .withBody("error_response_body")
                                .withStatusCode(502)
                );

    }

    @Test
    public void testFailureThenSuccess() {

        mockServer.reset();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(25);
        connectionManager.setDefaultMaxPerRoute(10);

        RestClientBuilder builder = new RestClientBuilder();
        RestClient client = builder
                .setClientTimeouts(30000,
                        30000,
                        30000)
                .setConnectionManager(connectionManager)
                .build();
        client.configureDestination("localhost", 9200, "http");

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
                        Times.exactly(2)
                )
                .respond(
                        response()
                                .withBody("normal_response_body")
                                .withStatusCode(200)
                );

        for(int i = 0; i < 3; i++){
            RestResult result = client.execQuery("/_nodes?pretty");
            logger.error(result.toString());
        }
    }

    @Test
    public void testAuthFailures() {

       /* mockServer.reset();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(25);
        connectionManager.setDefaultMaxPerRoute(10);

        RestClientBuilder builder = new RestClientBuilder();
        RestClient client = builder
                .setClientTimeouts(30000,
                        30000,
                        30000)
                .setConnectionManager(connectionManager)
                .build();
        client.configureDestination("localhost", 9200, "http");

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
                                .withStatusCode(401)
                );

        for(int i = 0; i < 3; i++){
            RestResult result = client.execQuery("/_nodes?pretty");
            logger.error(result.toString());
        }
  */  }


}
