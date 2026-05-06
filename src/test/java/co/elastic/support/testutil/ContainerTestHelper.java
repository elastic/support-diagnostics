/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.testutil;

import co.elastic.support.Constants;
import co.elastic.support.diagnostics.DiagConfig;
import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.diagnostics.DiagnosticInputs;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.rest.RestClient;
import co.elastic.support.util.JsonYamlUtils;
import co.elastic.support.util.ResourceCache;
import org.testcontainers.containers.GenericContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContainerTestHelper {

    public static final String STACK_VERSION =
            System.getenv("ELASTIC_STACK_VERSION") != null ? System.getenv("ELASTIC_STACK_VERSION") : "9.3.0";

    public static final Duration CONTAINER_STARTUP_TIMEOUT = Duration.ofMinutes(
            System.getenv("E2E_CONTAINER_STARTUP_TIMEOUT_MINUTES") != null
                    ? Long.parseLong(System.getenv("E2E_CONTAINER_STARTUP_TIMEOUT_MINUTES"))
                    : 3);

    public static RestClient clientFor(GenericContainer<?> container, int internalPort) {
        DiagConfig config = loadDiagConfig();
        return RestClient.getClient(
                container.getHost(),
                container.getMappedPort(internalPort),
                "http",
                "", "", "", Constants.DEEFAULT_PROXY_PORT, "", "", "", "",
                false,
                config.extraHeaders,
                config.connectionTimeout,
                config.connectionRequestTimeout,
                config.socketTimeout);
    }

    public static DiagnosticInputs inputsFor(GenericContainer<?> container, int internalPort,
                                             String diagType, Path outputDir) {
        DiagnosticInputs inputs = new DiagnosticInputs();
        inputs.host = container.getHost();
        inputs.port = container.getMappedPort(internalPort);
        inputs.scheme = "http";
        inputs.diagType = diagType;
        inputs.outputDir = outputDir.toString();
        return inputs;
    }

    public static DiagConfig loadDiagConfig() {
        try {
            return new DiagConfig(JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true));
        } catch (DiagnosticException e) {
            throw new RuntimeException("Failed to load diag config", e);
        }
    }

    public static DiagnosticContext contextFor(DiagnosticInputs inputs, ResourceCache cache) {
        return new DiagnosticContext(loadDiagConfig(), inputs, cache, false);
    }

    public static void assertZipContains(File zip, String... entryNames) throws IOException {
        try (ZipFile zipFile = new ZipFile(zip)) {
            Set<String> names = new HashSet<>();
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                names.add(entry.getName().replaceFirst("^(.+/)(.+)", "$2"));
            }
            for (String name : entryNames) {
                assertTrue(names.contains(name),
                        "Expected zip to contain '" + name + "' but found: " + names);
            }
        }
    }
}
