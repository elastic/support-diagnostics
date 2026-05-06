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
import co.elastic.support.diagnostics.commands.CheckKibanaVersion;
import co.elastic.support.rest.RestClient;
import co.elastic.support.util.ResourceCache;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.semver4j.Semver;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
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
class KibanaE2ETest {
    static final Network network = Network.newNetwork();

    @Container
    static final GenericContainer<?> ES_CONTAINER =
        new GenericContainer<>("docker.elastic.co/elasticsearch/elasticsearch:" + STACK_VERSION)
            .withEnv("discovery.type", "single-node")
            .withEnv("http.port", "19200")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withNetwork(network)
            .withNetworkAliases("elasticsearch-diag")
            .withExposedPorts(19200)
            .waitingFor(Wait.forHttp("/").forStatusCode(200).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));

    @Container
    static final GenericContainer<?> KIBANA_CONTAINER = new GenericContainer<>("docker.elastic.co/kibana/kibana:" + STACK_VERSION)
        .withEnv("ELASTICSEARCH_HOSTS", "http://elasticsearch-diag:19200")
        .withEnv("SERVER_PORT", "15601")
        .withEnv("XPACK_SECURITY_ENCRYPTIONKEY", "abcdefghijklmnopqrstuvwxyz123456")
        .withEnv("XPACK_ENCRYPTEDSAVEDOBJECTS_ENCRYPTIONKEY", "abcdefghijklmnopqrstuvwxyz123456")
        .withEnv("XPACK_REPORTING_ENCRYPTIONKEY", "abcdefghijklmnopqrstuvwxyz123456")
        .withNetwork(network)
        .withExposedPorts(15601)
        .waitingFor(Wait.forHttp("/api/status").forStatusCode(200).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));

    @Test
    void checkKibanaVersion() throws DiagnosticException {
        try (RestClient client = clientFor(KIBANA_CONTAINER, 15601)) {
            Semver version = CheckKibanaVersion.getKibanaVersion(client);
            assertTrue(version.getMajor() >= 8, "Expected version >= 8 but got: " + version);
        }
    }

    @Test
    void diagnosticServiceKibanaApiMode(@TempDir Path outputDir) throws DiagnosticException, IOException {
        DiagnosticInputs inputs = inputsFor(KIBANA_CONTAINER, 15601, Constants.kibanaApi, outputDir);
        try (ResourceCache cache = new ResourceCache()) {
            File zip = new DiagnosticService().exec(contextFor(inputs, cache));
            assertZipContains(zip, "diagnostic_manifest.json", "manifest.json");
        }
    }
}
