/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.e2e;

import co.elastic.support.Constants;
import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.diagnostics.DiagnosticInputs;
import co.elastic.support.diagnostics.DiagnosticService;
import co.elastic.support.diagnostics.commands.CheckLogstashVersion;
import co.elastic.support.rest.RestClient;
import co.elastic.support.util.ResourceCache;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.semver4j.Semver;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static co.elastic.support.testutil.ContainerTestHelper.CONTAINER_STARTUP_TIMEOUT;
import static co.elastic.support.testutil.ContainerTestHelper.STACK_VERSION;
import static co.elastic.support.testutil.ContainerTestHelper.assertZipContains;
import static co.elastic.support.testutil.ContainerTestHelper.clientFor;
import static co.elastic.support.testutil.ContainerTestHelper.contextFor;
import static co.elastic.support.testutil.ContainerTestHelper.inputsFor;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("e2e")
@Testcontainers
class LogstashE2ETest {
    @Container
    static final GenericContainer<?> LOGSTASH_CONTAINER =
        new GenericContainer<>("docker.elastic.co/logstash/logstash:" + STACK_VERSION)
            .withEnv("xpack.monitoring.enabled", "false")
            .withEnv("API_HTTP_PORT", "19600")
            .withEnv("LOG_LEVEL", "error")
            .withExposedPorts(19600)
            .waitingFor(Wait.forHttp("/").forStatusCode(200).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));

    @Test
    void checkLogstashVersion() throws DiagnosticException {
        try (RestClient client = clientFor(LOGSTASH_CONTAINER, 19600)) {
            Semver version = CheckLogstashVersion.getLogstashVersion(client);
            assertTrue(version.getMajor() >= 8, "Expected version >= 8 but got: " + version);
        }
    }

    @Test
    void diagnosticServiceLogstashApiMode(@TempDir Path outputDir) throws DiagnosticException, IOException {
        DiagnosticInputs inputs = inputsFor(LOGSTASH_CONTAINER, 19600, Constants.logstashApi, outputDir);
        try (ResourceCache cache = new ResourceCache()) {
            File zip = new DiagnosticService().exec(contextFor(inputs, cache));
            assertZipContains(zip, "diagnostic_manifest.json", "manifest.json");
        }
    }
}
