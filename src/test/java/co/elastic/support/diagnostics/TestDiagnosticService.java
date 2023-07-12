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
import org.junit.jupiter.api.*;
import org.junit.rules.TemporaryFolder;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestDiagnosticService {
    private ClientAndServer mockServer;

    private TemporaryFolder folder;

    static private String headerKey1 = "k1";
    static private String headerVal1 = "v1";
    static private String headerKey2 = "k2";
    static private String headerVal2 = "v2";

    @BeforeAll
    public void globalSetup() {
        mockServer = startClientAndServer(9880);
        // mockserver by default is in verbose mode (useful when creating new test), move it to warning.
        ConfigurationProperties.disableSystemOut(true);
        ConfigurationProperties.logLevel("WARN");
    }

    @AfterAll
    public void globalTeardown() {
        mockServer.stop();
    }

    @BeforeEach
    public void setup() throws IOException {
         folder = new TemporaryFolder();
         folder.create();
    }

    @AfterEach
    public void tearDown() {
        folder.delete();
    }

    private DiagConfig newDiagConfig() {
        Map diagMap = Collections.emptyMap();
        try {
            diagMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
        } catch (DiagnosticException e) {
            fail(e);
        }
        return new DiagConfig(diagMap);
    }

    private DiagnosticInputs newDiagnosticInputs() {
        DiagnosticInputs diagnosticInputs = new DiagnosticInputs();
        diagnosticInputs.port = 9880;
        diagnosticInputs.diagType = Constants.api;
        try {
            File outputDir = folder.newFolder();
            diagnosticInputs.outputDir = outputDir.toString();
        } catch(IOException e) {
            fail("Unable to create temp directory", e);
        }
        return diagnosticInputs;
    }

    private HttpRequest myRequest(Boolean withHeaders) {
        if (withHeaders) {
            return request().withHeaders(
                    new Header(headerKey1, headerVal1),
                    new Header(headerKey2, headerVal2)
            );
        } else {
            return request();
        }
    }

    private void setupResponse(Boolean withHeaders) {
        mockServer
                .when(
                        myRequest(withHeaders)
                                .withPath("/")
                )
                .respond(
                        response()
                                .withBody("{\"version\": {\"number\": \"7.14.0\"}}")
                );
        mockServer
                .when(
                        myRequest(withHeaders)
                                .withPath("/_nodes/os,process,settings,transport,http")
                )
                .respond(
                        response()
                                .withBody("{}")
                );
        mockServer
                .when(
                        myRequest(withHeaders)
                )
                .respond(
                        response()
                                .withBody("some_response_body")
                );
    }

    public HashMap<String, ZipEntry> zipFileContents(File result) throws IOException {
        ZipFile zipFile = new ZipFile(result, ZipFile.OPEN_READ);
        HashMap<String, ZipEntry> contents = new HashMap<>();

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            // Add file path without leading directory
            contents.put(entry.getName().replaceFirst("/[^/]*/", ""), entry);
        }
        return contents;
    }

    public void checkResult(File result, Boolean withLogFile) {
        assertTrue(result.toString().matches(".*\\.zip$"), result.toString());
        try {
            HashMap<String, ZipEntry> contents = zipFileContents(result);
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

        Map extraHeaders = new HashMap<String, String>();
        extraHeaders.put(headerKey1, headerVal1);
        extraHeaders.put(headerKey2, headerVal2);
        DiagConfig diagConfig = newDiagConfig();
        diagConfig.extraHeaders = extraHeaders;
        DiagnosticService diag = new DiagnosticService();

        try(
            ResourceCache resourceCache = new ResourceCache();
        ) {
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

        try(
            ResourceCache resourceCache = new ResourceCache();
        ) {
            DiagnosticContext context = new DiagnosticContext(newDiagConfig(), newDiagnosticInputs(), resourceCache, true);
            File result = diag.exec(context);
            checkResult(result, true);
        } catch (DiagnosticException e) {
            fail(e);
        }
    }

    @Test
    public void testConcurrentExecutions() {
        setupResponse(false);

        ConcurrentHashMap<Integer, File> results = new ConcurrentHashMap<Integer, File>();

        Function<Integer, Runnable> task = (Integer i) -> new Runnable() {
            @Override
            public void run() {
                DiagnosticService diag = new DiagnosticService();

                try(ResourceCache resourceCache = new ResourceCache()) {
                    DiagnosticContext context = new DiagnosticContext(newDiagConfig(), newDiagnosticInputs(), resourceCache, false);
                    File result = diag.exec(context);
                    results.put(i, result);
                } catch (DiagnosticException e) {
                    System.out.println(e.getStackTrace());
                    fail(e);
                }
            }
        };

        Thread[] threads = new Thread[3];
        Arrays.setAll(threads, i -> new Thread(task.apply(i)));
        Arrays.stream(threads).forEach(Thread::start);

        for (Thread t: threads) {
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
        assertEquals(results.size(), threads.length);
        results.forEach((i, result) -> checkResult(result, false));
        try {
            Enumeration<File> resultFiles = results.elements();
            // Take one zip file to use as a reference for comparisons with the other ones
            HashMap<String, ZipEntry> reference = zipFileContents(resultFiles.nextElement());
            while (resultFiles.hasMoreElements()) {
                HashMap<String, ZipEntry> other = zipFileContents(resultFiles.nextElement());
                assertEquals(reference.keySet(), other.keySet());
                reference.keySet().forEach((key) -> {
                    if (!key.equals("manifest.json")) {
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
