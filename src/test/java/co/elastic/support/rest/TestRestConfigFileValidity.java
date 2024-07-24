/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.rest;

import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.util.JsonYamlUtils;
import org.junit.jupiter.api.Test;
import org.semver4j.Semver;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRestConfigFileValidity {
    protected static Semver sem = new Semver("9.9.999");

    @Test
    public void validateElasticConfigVersioning() throws DiagnosticException {
        // validates each set of version entries.
        for (String yamlFile : Arrays.asList("elastic-rest.yml", "logstash-rest.yml", "kibana-rest.yml", "monitoring-rest.yml")) {
            Map<String, Object> restEntriesConfig = JsonYamlUtils.readYamlFromClasspath(yamlFile, true);
            validateEntries(yamlFile, restEntriesConfig);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateEntries(String file, Map<String, Object> config) {
        for (Map.Entry<String, Object> entry : config.entrySet()) {

            Map<String, Object> values = (Map<String, Object>) entry.getValue();
            Map<String, String> urls = (Map<String, String>) values.get("versions");

            int valid = 0;

            // Urls should have a leading /
            // For each entry there should be at most 1 valid url.
            for (Map.Entry<String, String> url : urls.entrySet()) {
                assertTrue(url.getValue().startsWith("/"), url.getValue());
                if (sem.satisfies(url.getKey())) {
                    valid++;
                }
            }

            // should be at most 1 valid URL (0 if it's not available anymore)
            assertTrue(valid <= 1, "[" + file +  "][" + entry.getKey() + "] matches " + valid);

        }

    }

}
