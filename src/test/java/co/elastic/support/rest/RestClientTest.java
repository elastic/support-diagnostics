/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.rest;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestClientTest {
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.getClient(
            "localhost",
            wm.getPort(),
            "http",
            null,
            null,
            null,
            0,
            null,
            null,
            null,
            null,
            false,
            null,
            5000,
            5000,
            5000
        );
    }

    @AfterEach
    void tearDown() {
        restClient.close();
    }

    @Test
    void execGet_returns200Response() {
        wm.stubFor(get(urlEqualTo("/_cat/health")).willReturn(aResponse().withStatus(200).withBody("green")));

        HttpResponse response = restClient.execGet("/_cat/health");
        try {
            assertEquals(200, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    @Test
    void execQuery_returnsValidRestResult() {
        wm.stubFor(get(urlEqualTo("/_cluster/health")).willReturn(aResponse().withStatus(200).withBody("{\"status\":\"green\"}")));

        RestResult result = restClient.execQuery("/_cluster/health");

        assertTrue(result.isValid());
        assertEquals("{\"status\":\"green\"}", result.toString());
    }

    @Test
    void execQuery_withNon200Status_returnsInvalidResult() {
        wm.stubFor(get(urlEqualTo("/_missing")).willReturn(aResponse().withStatus(404).withBody("Not Found")));

        RestResult result = restClient.execQuery("/_missing");

        assertFalse(result.isValid());
        assertEquals(404, result.getStatus());
        assertTrue(result.isRetryable());
    }

    @Test
    void execQuery_withFileName_writesResponseBodyToFile(@TempDir Path tempDir) throws IOException {
        wm.stubFor(get(urlEqualTo("/_cat/nodes")).willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "text/plain")
            .withBody("node-data")));

        Path outFile = tempDir.resolve("nodes.json");
        restClient.execQuery("/_cat/nodes", outFile.toString());

        assertTrue(Files.exists(outFile));
        assertEquals("node-data", new String(Files.readAllBytes(outFile)));
    }

    @Test
    void execPost_sendsJsonBodyAndContentTypeHeader() {
        String payload = "{\"query\":{\"match_all\":{}}}";
        wm.stubFor(post(urlEqualTo("/_search")).withHeader("Content-type", equalTo("application/json"))
            .withRequestBody(equalToJson(payload))
            .willReturn(aResponse().withStatus(200).withBody("{\"hits\":{}}")));

        HttpResponse response = restClient.execPost("/_search", payload);
        try {
            assertEquals(200, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.closeQuietly(response);
        }

        wm.verify(postRequestedFor(urlEqualTo("/_search")).withHeader("Accept", equalTo("application/json"))
            .withHeader("Content-type", equalTo("application/json")));
    }

    @Test
    void execDelete_sendsDeleteRequest() {
        wm.stubFor(delete(urlEqualTo("/my-index")).willReturn(aResponse().withStatus(200).withBody("{\"acknowledged\":true}")));

        HttpResponse response = restClient.execDelete("/my-index");
        try {
            assertEquals(200, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.closeQuietly(response);
        }

        wm.verify(deleteRequestedFor(urlEqualTo("/my-index")));
    }

    @Test
    void execGet_withExtraHeaders_includesHeadersInRequest() {
        wm.stubFor(get(urlEqualTo("/_cat/health")).willReturn(aResponse().withStatus(200).withBody("")));

        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("X-Custom-Header", "custom-value");

        try (
            RestClient clientWithHeaders = RestClient.getClient(
                "localhost",
                wm.getPort(),
                "http",
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                null,
                false,
                extraHeaders,
                5000,
                5000,
                5000
            )
        ) {
            HttpResponse response = clientWithHeaders.execGet("/_cat/health");
            HttpClientUtils.closeQuietly(response);
        }

        wm.verify(getRequestedFor(urlEqualTo("/_cat/health")).withHeader("X-Custom-Header", equalTo("custom-value")));
    }

    @Test
    void execGet_throwsRuntimeExceptionOnConnectionFailure() {
        try (
            RestClient badClient = RestClient.getClient(
                "localhost",
                19998,
                "http",
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                null,
                false,
                null,
                200,
                200,
                200
            )
        ) {
            assertThrows(RuntimeException.class, () -> badClient.execGet("/_cat/health"));
        }
    }

    @Test
    void close_delegatesToUnderlyingHttpClient() {
        boolean[] closed = { false };

        CloseableHttpClient trackingClient = new CloseableHttpClient() {
            @Override
            protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) {
                throw new UnsupportedOperationException();
            }

            @Override
            @SuppressWarnings("deprecation")
            public HttpParams getParams() {
                return null;
            }

            @Override
            @SuppressWarnings("deprecation")
            public ClientConnectionManager getConnectionManager() {
                return null;
            }

            @Override
            public void close() {
                closed[0] = true;
            }
        };

        RestClient client = new RestClient(trackingClient, new HttpHost("localhost"), HttpClientContext.create(), null);
        client.close();

        assertTrue(closed[0], "Expected underlying HTTP client to be closed");
    }

    @Test
    void close_calledTwice_doesNotThrow() {
        RestClient client = RestClient.getClient(
            "localhost",
            wm.getPort(),
            "http",
            null,
            null,
            null,
            0,
            null,
            null,
            null,
            null,
            false,
            null,
            5000,
            5000,
            5000
        );

        assertDoesNotThrow(() -> {
            client.close();
            client.close();
        });
    }

    @Test
    void getClient_withAuthentication_sendsBasicAuthHeader() {
        wm.stubFor(get(urlEqualTo("/_cat/health")).willReturn(aResponse().withStatus(200).withBody("green")));

        try (
            RestClient authClient = RestClient.getClient(
                "localhost",
                wm.getPort(),
                "http",
                "elastic",
                "changeme",
                null,
                0,
                null,
                null,
                null,
                null,
                false,
                null,
                5000,
                5000,
                5000
            )
        ) {

            HttpResponse response = authClient.execGet("/_cat/health");
            HttpClientUtils.closeQuietly(response);
        }

        wm.verify(getRequestedFor(urlEqualTo("/_cat/health")).withHeader("Authorization", containing("Basic")));
    }

    @Test
    void getClient_withBypassVerify_createsClientSuccessfully() {
        assertDoesNotThrow(() -> {
            try (
                RestClient httpsClient = RestClient.getClient(
                    "localhost",
                    9443,
                    "https",
                    null,
                    null,
                    null,
                    0,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null,
                    5000,
                    5000,
                    5000
                )
            ) {
                assertNotNull(httpsClient);
            }
        });
    }
}
