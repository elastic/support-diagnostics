/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.rest;

import co.elastic.support.Constants;
import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.util.JsonYamlUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestEntryConfigTest {
    private Map<String, Object> load(String resource) throws DiagnosticException {
        return JsonYamlUtils.readYamlFromClasspath(resource, true);
    }

    @Test
    void buildEntryMap_elasticsearch_containsCommonKeys() throws DiagnosticException {
        Map<String, Object> raw = load(Constants.ES_REST);
        Map<String, RestEntry> entries = new RestEntryConfig("9.3.0").buildEntryMap(raw);

        assertTrue(entries.containsKey("cat_health"), "Expected cat_health in ES entry map");
        assertTrue(entries.containsKey("cat_indices"), "Expected cat_indices in ES entry map");
        assertTrue(entries.containsKey("cat_nodes"), "Expected cat_nodes in ES entry map");
    }

    @Test
    void buildEntryMap_kibana_containsCommonKeys() throws DiagnosticException {
        Map<String, Object> raw = load(Constants.KIBANA_REST);
        Map<String, RestEntry> entries = new RestEntryConfig("9.3.0").buildEntryMap(raw);

        assertFalse(entries.isEmpty(), "Expected non-empty Kibana entry map");
        assertTrue(entries.containsKey("kibana_alerts_health"), "Expected kibana_alerts_health in map");
    }

    @Test
    void buildEntryMap_logstash_containsCommonKeys() throws DiagnosticException {
        Map<String, Object> raw = load(Constants.LS_REST);
        Map<String, RestEntry> entries = new RestEntryConfig("9.3.0").buildEntryMap(raw);

        assertTrue(entries.containsKey("logstash_node"), "Expected logstash_node in entry map");
        assertTrue(entries.containsKey("logstash_node_stats"), "Expected logstash_node_stats in entry map");
    }

    @Test
    void buildEntryMap_versionFilter_excludesOldOnlyEntries() throws DiagnosticException {
        Map<String, Object> raw = load(Constants.ES_REST);
        // cat_aliases has ">= 0.9.0 < 5.1.1" and ">= 5.1.1 < 7.7.0" and ">= 7.7.0" entries
        // building for 9.3.0 should include the ">= 7.7.0" variant, not the older ones
        Map<String, RestEntry> entries = new RestEntryConfig("9.3.0").buildEntryMap(raw);

        assertTrue(entries.containsKey("cat_aliases"), "cat_aliases should be present for 9.3.0");
        // The URL for >= 7.7.0 includes expand_wildcards
        assertTrue(entries.get("cat_aliases").getUrl().contains("expand_wildcards"),
                "Expected >=7.7.0 URL variant for cat_aliases");
    }

    @Test
    void buildEntryMap_modeFilter_excludesFullOnlyInLightMode() throws DiagnosticException {
        Map<String, Object> raw = load(Constants.ES_REST);
        Map<String, RestEntry> fullEntries = new RestEntryConfig("9.3.0", "full").buildEntryMap(raw);
        Map<String, RestEntry> lightEntries = new RestEntryConfig("9.3.0", "light").buildEntryMap(raw);

        assertTrue(fullEntries.size() >= lightEntries.size(),
                "Full mode should have >= entries compared to light mode");
    }
}
