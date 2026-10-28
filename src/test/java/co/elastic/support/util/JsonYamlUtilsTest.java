/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the Jackson behaviour the rest of the code base relies on, so that
 * a Jackson upgrade that changes it is caught here.
 */
class JsonYamlUtilsTest {

    @Test
    void createJsonNodeFromString_parsesObject() {
        JsonNode node = JsonYamlUtils.createJsonNodeFromString("{\"a\":{\"b\":\"c\"},\"n\":5,\"arr\":[1,2]}");

        assertTrue(node.isObject());
        assertEquals("c", node.path("a").path("b").asText());
        assertEquals("5", node.path("n").asText());
        assertTrue(node.path("arr").isArray());
        assertEquals(2, node.path("arr").size());
    }

    @Test
    void createJsonNodeFromString_invalidJson_throwsRuntimeException() {
        assertThrows(RuntimeException.class, () -> JsonYamlUtils.createJsonNodeFromString("{not json"));
    }

    @Test
    void createJsonNodeFromFileName_readsFile(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("x.json"), "{\"cluster_name\":\"prod\"}");

        JsonNode node = JsonYamlUtils.createJsonNodeFromFileName(dir.toString(), "x.json");

        assertEquals("prod", node.path("cluster_name").asText());
    }

    @Test
    void createJsonNodeFromFileName_missingFile_throwsRuntimeException(@TempDir Path dir) {
        assertThrows(RuntimeException.class,
                () -> JsonYamlUtils.createJsonNodeFromFileName(dir.toString(), "missing.json"));
    }

    @Test
    void path_onMissingField_asTextIsEmpty() {
        JsonNode node = JsonYamlUtils.createJsonNodeFromString("{\"a\":1}");

        assertTrue(node.path("nope").path("deeper").isMissingNode());
        assertEquals("", node.path("nope").path("deeper").asText());
    }

    @Test
    void asText_onExplicitNull_isNotNullReference() {
        JsonNode node = JsonYamlUtils.createJsonNodeFromString("{\"a\":null}");

        assertTrue(node.path("a").isNull());
        // Callers pass this through StringUtils / equals, so it must never be a Java null
        assertEquals(false, node.path("a").asText() == null);
    }

    @Test
    void properties_iteratesInDocumentOrder() {
        JsonNode node = JsonYamlUtils.createJsonNodeFromString("{\"z\":1,\"a\":2,\"m\":3}");

        List<String> keys = new ArrayList<>();
        node.properties().iterator().forEachRemaining(e -> keys.add(e.getKey()));

        assertEquals(List.of("z", "a", "m"), keys);
    }

    @Test
    void mapper_readValue_intoObjectNode_andMutateNestedPath() throws Exception {
        ObjectNode source = JsonYamlUtils.mapper.readValue(
                "{\"cluster_name\":\"old\",\"cluster_settings\":{\"cluster\":{\"metadata\":{\"display_name\":\"old\"}}}}",
                ObjectNode.class);

        source.put("cluster_name", "new");
        source.withObjectProperty("cluster_settings").withObjectProperty("cluster")
                .withObjectProperty("metadata").put("display_name", "new");

        String json = JsonYamlUtils.mapper.writeValueAsString(source);
        JsonNode reparsed = JsonYamlUtils.createJsonNodeFromString(json);
        assertEquals("new", reparsed.path("cluster_name").asText());
        assertEquals("new", reparsed.path("cluster_settings").path("cluster").path("metadata").path("display_name").asText());
        assertFalse(json.contains("\n"), "default mapper output is compact (one line per document for bulk ingest)");
    }

    @Test
    void mapper_withObjectProperty_createsMissingIntermediateObjects() {
        ObjectNode root = JsonYamlUtils.mapper.createObjectNode();

        root.withObjectProperty("a").withObjectProperty("b").put("c", "d");

        assertEquals("d", root.path("a").path("b").path("c").asText());
    }

    @Test
    void mapper_writeValueAsString_map() throws Exception {
        Map<String, Map<String, String>> action = new LinkedHashMap<>();
        action.put("create", Map.of("_index", "monitoring-es"));

        assertEquals("{\"create\":{\"_index\":\"monitoring-es\"}}", JsonYamlUtils.mapper.writeValueAsString(action));
    }

    @Test
    void mapper_convertValue_arrayToList() {
        JsonNode roles = JsonYamlUtils.createJsonNodeFromString("[\"superuser\",\"kibana_admin\"]");

        List<?> list = JsonYamlUtils.mapper.convertValue(roles, List.class);

        assertEquals(List.of("superuser", "kibana_admin"), list);
    }
}
