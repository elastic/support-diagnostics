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

import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestCheckLogstashVersionTest {
    private WireMockServer wireMockServer;
    private RestClient httpRestClient;

    @BeforeAll
    public void globalSetup() {
        wireMockServer = new WireMockServer(wireMockConfig().bindAddress("127.0.0.1").port(9881));
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
            9881,
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
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.resetAll();
    }

    private void initializeLogstashMainHandler(String version) {
        wireMockServer.stubFor(get(urlEqualTo("/")).willReturn(aResponse().withBody("{\"host\":\"Test\",\"version\":\"" + version
                + "\",\"http_address\":\"127.0.0.1:9600\",\"id\":\"9ac54ae7-377e-4352-9727-15db6344332a\",\"name\":\"LucaMBP\",\"ephemeral_id\":\"3f1d87db-07c0-4015-941a-4005bbf908fc\",\"snapshot\":false,\"status\":\"yellow\",\"pipeline\":{\"workers\":11,\"batch_size\":125,\"batch_delay\":50},\"build_date\":\"2025-06-17T14:07:37+00:00\",\"build_sha\":\"01b7a2d93e4cf143d4964c71259655cf4575b709\",\"build_snapshot\":false}")
            .withStatus(200)));
    }

    @Test
    public void testQueriesForLogstashVersionNormal() throws DiagnosticException {
        initializeLogstashMainHandler("8.1.2");
        Semver version = new CheckLogstashVersion().getLogstashVersion(httpRestClient);
        assertEquals("8.1.2", version.getVersion());
    }

    @Test
    public void testQueriesForLogstashVersionWithRC() throws DiagnosticException {
        initializeLogstashMainHandler("9.0.0-beta1");
        Semver version = new CheckLogstashVersion().getLogstashVersion(httpRestClient);
        assertEquals("9.0.0", version.getVersion());
    }

    @Test
    public void testQueriesForLogstashEmptyVersion() {
        wireMockServer.stubFor(get(urlEqualTo("/")).willReturn(aResponse().withBody("{}").withStatus(200)));

        try {
            new CheckLogstashVersion().getLogstashVersion(httpRestClient);
            fail("Expected to fail");
        } catch (DiagnosticException e) {
            assertEquals("Logstash version format is wrong - unable to continue. ()", e.getMessage());
        }
    }

    @Test
    public void testQueriesForLogstashCorruptedVersion() {
        initializeLogstashMainHandler("a.v.c");
        try {
            new CheckLogstashVersion().getLogstashVersion(httpRestClient);
            fail("Expected to fail");
        } catch (DiagnosticException e) {
            assertEquals("Logstash version format is wrong - unable to continue. (a.v.c)", e.getMessage());
        }
    }

    @Test
    public void testQueriesForLogstashTextWithVersion() {
        initializeLogstashMainHandler("test-6.5.1");

        try {
            new CheckLogstashVersion().getLogstashVersion(httpRestClient);
            fail("Expected to fail");
        } catch (DiagnosticException e) {
            assertEquals("Logstash version format is wrong - unable to continue. (test-6.5.1)", e.getMessage());
        }
    }
}
