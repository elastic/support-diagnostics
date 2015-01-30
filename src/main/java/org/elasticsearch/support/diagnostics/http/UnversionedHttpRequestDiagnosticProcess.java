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

import com.google.api.client.http.HttpRequestFactory;

import org.elasticsearch.support.diagnostics.settings.DiagnosticSettings;

/**
 * {@code UnversionedHttpRequestDiagnosticProcess} extends {@link HttpRequestDiagnosticProcess} for any unversioned
 * HTTP Request.
 * <p />
 * Extending this class instead of {@link HttpRequestDiagnosticProcess} implies that the current diagnostic does not
 * currently have any version restrictions and therefore it will always run as long as no exception occurs.
 */
public abstract class UnversionedHttpRequestDiagnosticProcess extends HttpRequestDiagnosticProcess {
    /**
     * Create a new {@link UnversionedHttpRequestDiagnosticProcess}.
     * <p />
     * The {@code encodedUrlSuffix} is used as the name by stripping off any querystring.
     *
     * @param encodedUrlSuffix The URL-encoded URL to append to the Elasticsearch base URL
     * @param filename The filename used to write the output
     * @param httpRequestFactory The HTTP Request factory for all requests
     * @throws NullPointerException if any parameter is {@code null}
     * @throws IllegalArgumentException if any parameter is blank
     */
    public UnversionedHttpRequestDiagnosticProcess(String encodedUrlSuffix,
                                                   String filename,
                                                   HttpRequestFactory httpRequestFactory) {
        super(encodedUrlSuffix, filename, httpRequestFactory);
    }

    /**
     * Create a new {@link UnversionedHttpRequestDiagnosticProcess}.
     *
     * @param name The name of the process
     * @param filename The filename used to write the output
     * @param httpRequestFactory The HTTP Request factory for all requests
     * @param encodedUrlSuffix The URL-encoded URL suffix to request
     * @throws NullPointerException if any parameter is {@code null}
     * @throws IllegalArgumentException if any parameter is blank
     */
    public UnversionedHttpRequestDiagnosticProcess(String name,
                                                   String filename,
                                                   HttpRequestFactory httpRequestFactory,
                                                   String encodedUrlSuffix) {
        super(name, filename, httpRequestFactory, encodedUrlSuffix);
    }

    /**
     * {@inheritDoc}
     *
     * @return Always {@code true}.
     */
    @Override
    protected boolean isUsable(DiagnosticSettings settings) {
        return true;
    }
}
