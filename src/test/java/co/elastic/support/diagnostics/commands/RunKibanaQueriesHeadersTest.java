/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.util.JsonYamlUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunKibanaQueriesHeadersTest {

    private JsonNode filter(Path dir, String json) throws Exception {
        Files.writeString(dir.resolve("kibana_actions.json"), json);
        DiagnosticContext context = new DiagnosticContext(null, null, null, false);
        context.tempDir = dir.toString();

        new RunKibanaQueries().filterActionsHeaders(context);

        return JsonYamlUtils.createJsonNodeFromString(Files.readString(dir.resolve("kibana_actions.json")));
    }

    @Test
    void removesSensitiveHeaders_keepsAllowedOnes_caseInsensitively(@TempDir Path dir) throws Exception {
        JsonNode result = filter(dir, "[{\"id\":\"a\",\"config\":{\"url\":\"http://x\",\"headers\":"
                + "{\"Authorization\":\"Bearer secret\",\"kbn-xsrf\":\"true\",\"Content-Type\":\"application/json\",\"X-Api-Key\":\"k\"}}}]");

        JsonNode headers = result.get(0).path("config").path("headers");
        assertEquals(2, headers.size());
        assertTrue(headers.has("kbn-xsrf"));
        assertTrue(headers.has("Content-Type"));
        assertFalse(headers.has("Authorization"));
        assertFalse(headers.has("X-Api-Key"));
        assertEquals("http://x", result.get(0).path("config").path("url").asString());
    }

    @Test
    void handlesMultipleActions_withAndWithoutConfigOrHeaders(@TempDir Path dir) throws Exception {
        JsonNode result = filter(dir, "[{\"id\":\"1\"},{\"id\":\"2\",\"config\":null},{\"id\":\"3\",\"config\":{}},"
                + "{\"id\":\"4\",\"config\":{\"headers\":null}},{\"id\":\"5\",\"config\":{\"headers\":{\"Authorization\":\"s\"}}}]");

        assertEquals(5, result.size());
        assertEquals(0, result.get(4).path("config").path("headers").size());
        assertTrue(result.get(3).path("config").path("headers").isNull());
    }

    @Test
    void leavesFileUntouched_whenNothingToRemove(@TempDir Path dir) throws Exception {
        String original = "[{\"id\":\"a\",\"config\":{\"headers\":{\"kbn-xsrf\":\"true\"}}}]";
        Files.writeString(dir.resolve("kibana_actions.json"), original);
        DiagnosticContext context = new DiagnosticContext(null, null, null, false);
        context.tempDir = dir.toString();

        new RunKibanaQueries().filterActionsHeaders(context);

        assertEquals(original, Files.readString(dir.resolve("kibana_actions.json")));
    }

    @Test
    void emptyActionsArray_isNoOp(@TempDir Path dir) throws Exception {
        JsonNode result = filter(dir, "[]");

        assertEquals(0, result.size());
    }
}
