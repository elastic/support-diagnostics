/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics;

import co.elastic.support.Constants;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.util.JsonYamlUtils;
import co.elastic.support.util.ResourceCache;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestDiagnosticService {
    private WireMockServer wireMockServer;

    @TempDir
    private Path folder;

    private static final String headerKey1 = "k1";
    private static final String headerVal1 = "v1";
    private static final String headerKey2 = "k2";
    private static final String headerVal2 = "v2";

    @BeforeAll
    public void globalSetup() {
        wireMockServer = new WireMockServer(wireMockConfig().bindAddress("127.0.0.1").port(9880));
        wireMockServer.start();
    }

    @AfterAll
    public void globalTeardown() {
        wireMockServer.stop();
    }

    private DiagConfig newDiagConfig() {
        try {
            return new DiagConfig(
                    JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true));
        } catch (DiagnosticException e) {
            fail(e);
            return null; // unreachable because of fail(e)
        }
    }

    private DiagnosticInputs newDiagnosticInputs() {
        DiagnosticInputs diagnosticInputs = new DiagnosticInputs();
        diagnosticInputs.port = 9880;
        diagnosticInputs.scheme = "http";
        diagnosticInputs.diagType = Constants.api;
        try {
            Path outputDir = Files.createTempDirectory(folder, "diag");
            diagnosticInputs.outputDir = outputDir.toString();
        } catch (IOException e) {
            fail("Unable to create temp directory", e);
        }
        return diagnosticInputs;
    }

    private void setupResponse(boolean withHeaders) {
        if (withHeaders) {
            wireMockServer.stubFor(any(urlEqualTo("/"))
                    .withHeader(headerKey1, equalTo(headerVal1))
                    .withHeader(headerKey2, equalTo(headerVal2))
                    .willReturn(aResponse()
                            .withBody("{\"version\": {\"number\": \"7.14.0\"}}")));
            wireMockServer.stubFor(any(urlEqualTo("/_nodes/os,process,settings,transport,http"))
                    .withHeader(headerKey1, equalTo(headerVal1))
                    .withHeader(headerKey2, equalTo(headerVal2))
                    .willReturn(aResponse()
                            .withBody("{}")));
            wireMockServer.stubFor(any(anyUrl())
                    .withHeader(headerKey1, equalTo(headerVal1))
                    .withHeader(headerKey2, equalTo(headerVal2))
                    .atPriority(10)
                    .willReturn(aResponse()
                            .withBody("some_response_body")));
        } else {
            wireMockServer.stubFor(any(urlEqualTo("/"))
                    .willReturn(aResponse()
                            .withBody("{\"version\": {\"number\": \"7.14.0\"}}")));
            wireMockServer.stubFor(any(urlEqualTo("/_nodes/os,process,settings,transport,http"))
                    .willReturn(aResponse()
                            .withBody("{}")));
            wireMockServer.stubFor(any(anyUrl())
                    .atPriority(10)
                    .willReturn(aResponse()
                            .withBody("some_response_body")));
        }
    }

    public HashMap<String, ZipEntry> zipFileContents(File result) throws IOException {
        try (ZipFile zipFile = new ZipFile(result, ZipFile.OPEN_READ)) {
            HashMap<String, ZipEntry> contents = new HashMap<>();

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                assertFalse(entry.getName().startsWith("./"), entry.getName());

                if (!entry.isDirectory()) {
                    // Add file path without leading directory
                    contents.put(entry.getName().replaceFirst("^(.+/)(.+)", "$2"), entry);
                }
            }

            return contents;
        }
    }

    public void checkResult(File result, boolean withLogFile) {
        assertTrue(result.toString().matches(".*\\.zip$"), result.toString());
        try {
            Map<String, ZipEntry> contents = zipFileContents(result);

            assertTrue(contents.containsKey("diagnostic_manifest.json"),
                    () -> String.join(", ", contents.keySet()));

            assertTrue(contents.containsKey("manifest.json"));
            if (withLogFile) {
                assertTrue(contents.containsKey("diagnostics.log"));
            } else {
                assertFalse(contents.containsKey("diagnostics.log"));
            }
            contents.forEach((key, entry) -> {
                if (!entry.isDirectory()) {
                    assertTrue(entry.getSize() > 0, key);
                }
            });
        } catch (IOException e) {
            fail("Error processing result zip file", e);
        }
    }

    @Test
    public void testWithExtraHeaders() {
        setupResponse(true);

        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put(headerKey1, headerVal1);
        extraHeaders.put(headerKey2, headerVal2);
        DiagConfig diagConfig = newDiagConfig();
        diagConfig.extraHeaders = extraHeaders;
        DiagnosticService diag = new DiagnosticService();

        try (ResourceCache resourceCache = new ResourceCache()) {
            DiagnosticContext context = new DiagnosticContext(diagConfig, newDiagnosticInputs(), resourceCache, true);
            File result = diag.exec(context);
            checkResult(result, true);
        } catch (DiagnosticException e) {
            fail(e);
        }
    }

    @Test
    public void testWithoutExtraHeaders() {
        setupResponse(false);

        DiagnosticService diag = new DiagnosticService();

        try (ResourceCache resourceCache = new ResourceCache()) {
            DiagnosticContext context = new DiagnosticContext(newDiagConfig(), newDiagnosticInputs(), resourceCache,
                    true);
            File result = diag.exec(context);
            checkResult(result, true);
        } catch (DiagnosticException e) {
            fail(e);
        }
    }

    @Test
    public void testConcurrentExecutions() {
        setupResponse(false);

        ConcurrentHashMap<Integer, File> results = new ConcurrentHashMap<>();

        Function<Integer, Runnable> task = (Integer i) -> () -> {
            DiagnosticService diag = new DiagnosticService();

            try (ResourceCache resourceCache = new ResourceCache()) {
                DiagnosticContext context = new DiagnosticContext(newDiagConfig(), newDiagnosticInputs(),
                        resourceCache, false);
                File result = diag.exec(context);
                results.put(i, result);
            } catch (DiagnosticException e) {
                System.out.println(e.getStackTrace());
                fail(e);
            }
        };

        List<Thread> threads = List.of(new Thread(task.apply(0)), new Thread(task.apply(1)), new Thread(task.apply(2)));
        threads.forEach(Thread::start);

        for (Thread t : threads) {
            try {
                t.join(3000);
            } catch (InterruptedException e) {
                fail("Thread interrupted", e);
            } finally {
                if (t.isAlive()) {
                    t.interrupt();
                    fail("Thread got stuck");
                }
            }
        }
        assertEquals(results.size(), threads.size());
        results.forEach((i, result) -> checkResult(result, false));
        try {
            Enumeration<File> resultFiles = results.elements();
            // Take one zip file to use as a reference for comparisons with the other ones
            Map<String, ZipEntry> reference = zipFileContents(resultFiles.nextElement());
            while (resultFiles.hasMoreElements()) {
                Map<String, ZipEntry> other = zipFileContents(resultFiles.nextElement());
                assertEquals(reference.keySet(), other.keySet(), () -> reference.keySet().stream()
                        .filter(file -> !other.containsKey(file)).collect(Collectors.joining(", ")));
                reference.keySet().forEach((key) -> {
                    if (!key.equals("manifest.json") && !key.equals("diagnostic_manifest.json")) {
                        assertEquals(reference.get(key).getSize(), other.get(key).getSize(), key);
                        assertEquals(reference.get(key).getCrc(), other.get(key).getCrc(), key);
                    }
                });
            }
        } catch (IOException e) {
            fail("Exception while comparing result contents", e);
        }
    }
}
