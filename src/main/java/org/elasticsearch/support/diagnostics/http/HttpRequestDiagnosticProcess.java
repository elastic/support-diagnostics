/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.support.diagnostics.http;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;

import org.elasticsearch.support.diagnostics.AbstractDiagnosticProcess;
import org.elasticsearch.support.diagnostics.settings.DiagnosticSettings;
import org.elasticsearch.support.diagnostics.preprocessor.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code HttpRequestDiagnosticProcess} provides a simple mechanism to make a standard HTTP Request and write its
 * complete HTTP Response.
 * <p />
 * Errors that may occur during (or after) a request are caught, logged, and written to complementary
 * <code>.error</code> files..
 */
public abstract class HttpRequestDiagnosticProcess extends AbstractDiagnosticProcess {
    /**
     * {@link HttpRequestDiagnosticProcess} logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestDiagnosticProcess.class);

    /**
     * The {@link HttpRequestFactory} enables creation of {@link HttpRequest}s on demand.
     */
    private final HttpRequestFactory httpRequestFactory;
    /**
     * The value to append to the Elasticsearch URL to perform the diagnostic lookup.
     */
    private final String encodedUrlSuffix;

    /**
     * Create a new {@link HttpRequestDiagnosticProcess}.
     *
     * @param encodedUrlSuffix The URL-encoded URL suffix to request, which is used as the name by cutting any
     *                         querystring
     * @param filename The filename used to write the output
     * @param httpRequestFactory The HTTP Request factory for all requests
     * @throws NullPointerException if any parameter is {@code null}
     * @throws IllegalArgumentException if any parameter is blank
     */
    public HttpRequestDiagnosticProcess(String encodedUrlSuffix,
                                        String filename,
                                        HttpRequestFactory httpRequestFactory) {
        // chop off any querystring
        this(encodedUrlSuffix.substring(0, encodedUrlSuffix.indexOf('?')),
             filename,
             httpRequestFactory,
             encodedUrlSuffix);
    }

    /**
     * Create a new {@link HttpRequestDiagnosticProcess}.
     *
     * @param name The name of the process
     * @param filename The filename used to write the output
     * @param httpRequestFactory The HTTP Request factory for all requests
     * @param encodedUrlSuffix The URL-encoded URL suffix to request
     * @throws NullPointerException if any parameter is {@code null}
     * @throws IllegalArgumentException if any parameter is blank
     */
    public HttpRequestDiagnosticProcess(String name,
                                        String filename,
                                        HttpRequestFactory httpRequestFactory,
                                        String encodedUrlSuffix) {
        super(name, filename);

        checkArgument( ! encodedUrlSuffix.trim().isEmpty(), "encodedUrlSuffix cannot be blank");

        // required
        this.httpRequestFactory = checkNotNull(httpRequestFactory, "httpRequestFactory cannot be null");
        this.encodedUrlSuffix = encodedUrlSuffix;
    }

    /**
     * Get the URL-encoded URL suffix to append to the base URL.
     *
     * @return Never blank. Always the same.
     */
    public String getEncodedUrlSuffix() {
        return encodedUrlSuffix;
    }
    /**
     * {@inheritDoc}
     * <p />
     * Configuring when to run should be done by overriding {@link #isUsable(DiagnosticSettings)}.
     *
     * @return {@code true} if {@link #isUsable(DiagnosticSettings)} returns {@code true} so that the request runs.
     *         {@code false} otherwise.
     * @throws NullPointerException if {@code settings} is {@code null}
     * @throws RuntimeException if the HTTP request fails or the file cannot be written
     */
    @Override
    public final boolean run(DiagnosticSettings settings) {
        boolean run = isUsable(settings);

        // versions checked out?
        if (run) {
            doRun(settings);
        }

        return run;
    }

    /**
     * Determine if the {@code this} {@link VersionedHttpRequestDiagnosticProcess} should be {@link #doRun run} with
     * the given {@code settings}.
     *
     * @param settings The settings to check (e.g., the version)
     * @return {@code true} to run the diagnostic. {@code false} otherwise.
     */
    protected abstract boolean isUsable(DiagnosticSettings settings);

    /**
     * Executes the logic associated with {@link #run} without requiring a return value.
     *
     * @param settings Setting/configuration parameters supplied by the user.
     * @throws NullPointerException if {@code settings} is {@code null}
     * @throws RuntimeException if the HTTP request fails or the file cannot be written
     * @see #run(DiagnosticSettings)
     */
    protected void doRun(DiagnosticSettings settings) {
        downloadHttpResponseToFile(settings, getEncodedUrlSuffix());
    }

    /**
     * Create a GET {@link HttpRequest} with the {@code encodedUrl}.
     *
     * @param encodedUrl The full URL-encoded URL
     * @return Never {@code null}.
     * @throws RuntimeException if any error occurs (unexpected)
     * @throws IllegalArgumentException if {@code encodedUrl} is invalidly encoded
     */
    protected HttpRequest createHttpGetRequest(String encodedUrl) {
        try {
            return httpRequestFactory.buildGetRequest(new GenericUrl(encodedUrl));
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to create HTTP Request for " + encodedUrl, e);
        }
    }

    /**
     * Executes a GET {@link HttpRequest} by appending the {@code encodedUrlSuffix} to the base Elasticsearch URL from
     * the {@code settings}, executing the request, and writing its response to the {@link #getFilename() filename}
     * within the output directory from the {@code settings}.
     *
     * @param settings The settings given to the diagnostic process
     * @param encodedUrlSuffix The URL-encoded URL suffix to request (e.g., <code>"_mapping"</code> for
     *                         <code>"http://localhost:9200/_mapping"</code>)
     * @throws NullPointerException if any parameter is {@code null}
     * @throws RuntimeException if the HTTP request fails or the file cannot be written
     */
    protected void downloadHttpResponseToFile(DiagnosticSettings settings, String encodedUrlSuffix) {
        downloadHttpResponseToFile(settings.get(Names.BASE_ELASTICSEARCH_URL) + encodedUrlSuffix,
                                   Paths.get(settings.get(Names.OUTPUT_DIRECTORY), getFilename()));
    }

    /**
     * Executes a GET {@link HttpRequest} for the {@code encodedUrl} and downloads the response into the specified
     * {@code outputFile}.
     *
     * @param encodedUrl The full URL-encoded URL
     * @param outputFile The path to the file to write
     * @throws NullPointerException if any parameter is {@code null}
     * @throws RuntimeException if the HTTP request fails or the file cannot be written
     */
    protected void downloadHttpResponseToFile(String encodedUrl, Path outputFile) {
        downloadHttpResponseToFile(createHttpGetRequest(encodedUrl), outputFile);
    }

    /**
     * Executes the {@code request} and downloads the response into the specified {@code outputFile}.
     *
     * @param request The HTTP Request whose Response is written to the {@code outputFile}.
     * @param outputFile The path to the file to write
     * @throws NullPointerException if any parameter is {@code null}
     * @throws RuntimeException if the HTTP request fails or the file cannot be written
     */
    protected void downloadHttpResponseToFile(HttpRequest request, Path outputFile) {
        LOGGER.debug("Requesting [{} {}] to write to [{}]", request.getRequestMethod(), request.getUrl(), outputFile);

        HttpResponse response = null;

        // write the outputFile to disk using the response from the request
        try (FileOutputStream file = new FileOutputStream(outputFile.toFile())) {
            // fetch the response
            response = request.execute();

            // write the response to disk
            response.download(file);
        }
        catch (IOException e) {
            // attempts to write a meaningful message whether the server responded or not
            StringBuilder message =
                    new StringBuilder("Unable to write response for '").append(request.getUrl()).append("'. ");

            // if we actually get a response, then write the status code to disk
            if (response != null) {
                message.append("Status Code: ").append(response.getStatusCode());
            }
            else {
                message.append("Ensure that this machine can see the specified host/URL.");
            }

            throw rethrowableException(outputFile, message.toString(), e);
        }
    }
}
