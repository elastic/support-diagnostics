/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.rest;

import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.util.JsonYamlUtils;
import com.vdurmont.semver4j.Semver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.*;

public class TestRestConfigFileValidity {

    private static final Logger logger = LogManager.getLogger(TestRestConfigFileValidity.class);

    protected static Semver sem = new Semver("9.9.999", Semver.SemverType.NPM);

    @Test
    public void validateElasticConfigVersioning() throws DiagnosticException {
        // validates each set of version entries.
        for (String yamlfile : Arrays.asList("elastic-rest.yml", "logstash-rest.yml", "kibana-rest.yml", "monitoring-rest.yml")) {
            Map<String, Object> restEntriesConfig = JsonYamlUtils.readYamlFromClasspath(yamlfile, true);
            validateEntries(yamlfile, restEntriesConfig);
        }
    }


    private void validateEntries(String file, Map<String, Object> config) {
        for (Map.Entry<String, Object> entry : config.entrySet()) {

            Map<String, Object> values = (Map) entry.getValue();

            Map<String, String> urls = (Map) values.get("versions");

            int nbrValid = 0;

            // Urls should have a leading /
            // For each entry there should be at most 1 valid url.
            for (Map.Entry<String, String> url : urls.entrySet()) {
                assertTrue(url.getValue(), url.getValue().startsWith("/"));
                if (sem.satisfies(url.getKey())) {
                    nbrValid++;
                }
            }

            // should be at most 1 valid URL (0 if it's not available anymore)
            assertTrue("[" + file +  "][" + entry.getKey() + "] matches " + nbrValid, nbrValid <= 1);

        }

    }

}
