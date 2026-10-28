/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.diagnostics.DiagnosticInputs;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.util.JsonYamlUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManifestCommandsTest {

    private DiagnosticContext context(Path dir, String diagType) {
        DiagnosticInputs inputs = new DiagnosticInputs("test-runner");
        inputs.diagType = diagType;
        DiagnosticContext context = new DiagnosticContext(null, inputs, null, false);
        context.tempDir = dir.toString();
        context.diagVersion = "9.9.9";
        return context;
    }

    @Test
    void generateManifest_writesIndentedJson(@TempDir Path dir) throws Exception {
        new GenerateManifest().execute(context(dir, "local"));

        String raw = Files.readString(dir.resolve("manifest.json"));
        assertTrue(raw.contains("\n"), "INDENT_OUTPUT should pretty-print");
        JsonNode json = JsonYamlUtils.createJsonNodeFromString(raw);
        assertEquals("test-runner", json.path("runner").asText());
        assertTrue(json.has("collectionDate"));
    }

    @Test
    void generateDiagnosticManifest_writesIndentedJson_andDetectsProduct(@TempDir Path dir) throws Exception {
        new GenerateDiagnosticManifest().execute(context(dir, "kibana-api"));

        String raw = Files.readString(dir.resolve("diagnostic_manifest.json"));
        assertTrue(raw.contains("\n"), "INDENT_OUTPUT should pretty-print");
        JsonNode json = JsonYamlUtils.createJsonNodeFromString(raw);
        assertEquals("kibana", json.path("product").asText());
        assertEquals("kibana_diagnostic", json.path("type").asText());
        assertEquals("9.9.9", json.path("diagnostic").asText());
        assertEquals("test-runner", json.path("runner").asText());
    }
}
