/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.rest;

import co.elastic.support.diagnostics.DiagnosticInputs;
import co.elastic.support.diagnostics.commands.CheckUserAuthLevel;
import co.elastic.support.util.TextIOManager;
import java.util.List;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyAuthTest {
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @Test
    void headerValue_encodedKeyIsUsedAsIs() {
        assertEquals("ApiKey VnVhQ2ZHY0JDZGJrUW0tZTVhT3g6dWkybHAyYXhUTm1zeWFrdzl0dk5udw==",
                ElasticRestClientInputs.apiKeyHeaderValue("VnVhQ2ZHY0JDZGJrUW0tZTVhT3g6dWkybHAyYXhUTm1zeWFrdzl0dk5udw=="));
    }

    @Test
    void headerValue_idAndKeyPairIsBase64Encoded() {
        // base64("abc:def") = YWJjOmRlZg==
        assertEquals("ApiKey YWJjOmRlZg==", ElasticRestClientInputs.apiKeyHeaderValue("abc:def"));
    }

    @Test
    void headerValue_trimsWhitespaceAndPrefix() {
        assertEquals("ApiKey YWJjOmRlZg==", ElasticRestClientInputs.apiKeyHeaderValue("  ApiKey YWJjOmRlZg==\n"));
    }

    @Test
    void requestHeaders_withoutKeyReturnsConfigHeadersUntouched() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        Map<String, String> config = Collections.singletonMap("X-Test", "1");
        assertSame(config, inputs.requestHeaders(config));
        assertNull(inputs.requestHeaders(null));
    }

    @Test
    void requestHeaders_withKeyAddsAuthorizationAndKeepsConfigHeaders() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        inputs.apiKey = "abc:def";
        Map<String, String> config = new HashMap<>();
        config.put("X-Test", "1");

        Map<String, String> headers = inputs.requestHeaders(config);

        assertEquals("ApiKey YWJjOmRlZg==", headers.get("Authorization"));
        assertEquals("1", headers.get("X-Test"));
        assertFalse(config.containsKey("Authorization"), "config map must not be mutated");
        assertEquals("ApiKey YWJjOmRlZg==", inputs.requestHeaders(null).get("Authorization"));
    }

    @Test
    void validate_rejectsApiKeyWithUserOrPassword() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        inputs.apiKey = "abc:def";
        assertNull(inputs.validateApiKeyCombination());

        inputs.user = "elastic";
        assertNotNull(inputs.validateApiKeyCombination());

        inputs.user = "";
        inputs.password = "secret";
        assertNotNull(inputs.validateApiKeyCombination());

        inputs.password = "";
        inputs.pkiKeystore = "/tmp/store.p12";
        assertNull(inputs.validateApiKeyCombination(), "PKI carries the TLS client cert, the key authenticates");
    }

    @Test
    void validateFormat_rejectsBlankAndWhitespace() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        assertNotNull(inputs.validateApiKeyFormat(""));
        assertNotNull(inputs.validateApiKeyFormat("   "));
        assertNotNull(inputs.validateApiKeyFormat("abc def"));
        assertNotNull(inputs.validateApiKeyFormat("abc\ndef"));
        assertNull(inputs.validateApiKeyFormat("YWJjOmRlZg=="));
        assertNull(inputs.validateApiKeyFormat("ApiKey YWJjOmRlZg=="));
    }

    @Test
    void headerValue_prefixIsCaseInsensitiveAndWorksWithIdKeyPair() {
        assertEquals("ApiKey YWJjOmRlZg==", ElasticRestClientInputs.apiKeyHeaderValue("apikey YWJjOmRlZg=="));
        assertEquals("ApiKey YWJjOmRlZg==", ElasticRestClientInputs.apiKeyHeaderValue("ApiKey abc:def"));
    }

    @Test
    void requestHeaders_replacesConfiguredAuthorizationInAnyCase() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        inputs.apiKey = "abc:def";
        Map<String, String> config = new HashMap<>();
        config.put("authorization", "ApiKey old");

        Map<String, String> headers = inputs.requestHeaders(config);

        assertEquals(1, headers.size());
        assertEquals("ApiKey YWJjOmRlZg==", headers.get("Authorization"));
    }

    @Test
    void parseInputs_blankKeyIsAnError() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        List<String> errors = inputs.parseInputs(new TextIOManager(),
                new String[] {"--host", "localhost", "--type", "api", "--apiKeyText", " "});
        assertTrue(errors.stream().anyMatch(e -> e.contains("API key is empty")), errors.toString());
    }

    @Test
    void parseInputs_keyWithUserIsAnError() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        List<String> errors = inputs.parseInputs(new TextIOManager(),
                new String[] {"--host", "localhost", "--type", "api", "-u", "elastic", "--apiKeyText", "abc:def"});
        assertTrue(errors.stream().anyMatch(e -> e.contains("cannot be combined")), errors.toString());
    }

    @Test
    void parseInputs_validKeyHasNoErrorsAndIsUsed() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        List<String> errors = inputs.parseInputs(new TextIOManager(),
                new String[] {"--host", "localhost", "--type", "api", "--apiKeyText", "abc:def"});
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(inputs.usesApiKey());
    }

    @Test
    void privilegeCheck_onlyAFullYesCounts() {
        assertTrue(CheckUserAuthLevel.hasAllRequestedPrivileges("{\"has_all_requested\":true}"));
        assertFalse(CheckUserAuthLevel.hasAllRequestedPrivileges("{\"has_all_requested\":false}"));
        assertFalse(CheckUserAuthLevel.hasAllRequestedPrivileges("{}"));
    }

    @Test
    void toString_neverContainsTheKey() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        inputs.apiKey = "supersecretkey";
        assertFalse(inputs.toString().contains("supersecretkey"));
    }

    @Test
    void restClient_sendsAuthorizationHeaderOnGetAndPost() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        inputs.apiKey = "abc:def";
        wm.stubFor(get(urlEqualTo("/")).willReturn(aResponse().withStatus(200).withBody("{}")));
        wm.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/p"))
                .willReturn(aResponse().withStatus(200).withBody("{}")));

        try (RestClient client = RestClient.getClient("localhost", wm.getPort(), "http", "", "", "", 0, "", "", "", "",
                false, inputs.requestHeaders(null), 5000, 5000, 5000)) {
            assertTrue(client.execQuery("/").isValid());
            client.execPost("/p", "{}");
        }

        wm.verify(getRequestedFor(urlEqualTo("/")).withHeader("Authorization", equalTo("ApiKey YWJjOmRlZg==")));
        wm.verify(com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(urlEqualTo("/p"))
                .withHeader("Authorization", equalTo("ApiKey YWJjOmRlZg==")));
    }
}
