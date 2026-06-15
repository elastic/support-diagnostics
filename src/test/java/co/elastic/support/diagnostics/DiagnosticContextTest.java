/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics;

import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.util.ResourceCache;
import org.junit.jupiter.api.Test;

import static co.elastic.support.testutil.ContainerTestHelper.loadDiagConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticContextTest {

    @Test
    void construction_setsAllFields() {
        DiagConfig config = loadDiagConfig();
        DiagnosticInputs inputs = new DiagnosticInputs();
        try (ResourceCache cache = new ResourceCache()) {
            DiagnosticContext context = new DiagnosticContext(config, inputs, cache, true);

            assertSame(config, context.diagsConfig);
            assertSame(inputs, context.diagnosticInputs);
            assertSame(cache, context.resourceCache);
            assertTrue(context.includeLogs);
        }
    }

    @Test
    void defaultValues_runSystemCalls() {
        try (ResourceCache cache = new ResourceCache()) {
            DiagnosticContext context = new DiagnosticContext(
                    loadDiagConfig(), new DiagnosticInputs(), cache, false);
            assertTrue(context.runSystemCalls);
        }
    }

    @Test
    void defaultValues_isAuthorized() {
        try (ResourceCache cache = new ResourceCache()) {
            DiagnosticContext context = new DiagnosticContext(
                    loadDiagConfig(), new DiagnosticInputs(), cache, false);
            assertTrue(context.isAuthorized);
        }
    }

    @Test
    void defaultValues_dockerPresent() {
        try (ResourceCache cache = new ResourceCache()) {
            DiagnosticContext context = new DiagnosticContext(
                    loadDiagConfig(), new DiagnosticInputs(), cache, false);
            assertFalse(context.dockerPresent);
        }
    }

    @Test
    void includeLogs_false() {
        try (ResourceCache cache = new ResourceCache()) {
            DiagnosticContext context = new DiagnosticContext(
                    loadDiagConfig(), new DiagnosticInputs(), cache, false);
            assertFalse(context.includeLogs);
        }
    }

    @Test
    void clusterName_defaultsToEmpty() {
        try (ResourceCache cache = new ResourceCache()) {
            DiagnosticContext context = new DiagnosticContext(
                    loadDiagConfig(), new DiagnosticInputs(), cache, false);
            assertEquals("", context.clusterName);
        }
    }
}
