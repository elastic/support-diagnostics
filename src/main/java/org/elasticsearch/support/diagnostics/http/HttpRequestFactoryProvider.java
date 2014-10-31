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
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.net.Proxy;

/**
 * {@code HttpRequestFactoryProvider} provides a singleton {@link HttpRequestFactory} to be used for any
 * {@code HttpRequest} creation.
 */
public class HttpRequestFactoryProvider implements Provider<HttpRequestFactory> {
    /**
     * The optional {@link Proxy}.
     */
    private final Proxy proxy;
    /**
     * The optional {@link SSLSocketFactory}.
     */
    private final SSLSocketFactory sslSocketFactory;
    /**
     * The optional {@link HostnameVerifier}.
     */
    private final HostnameVerifier hostnameVerifier;
    /**
     * The optional {@link HttpRequestInitializer}.
     * <p />
     * This enables manipulation of the generated {@code HttpRequest}s. In documented examples, they use one to disable
     * the request timeout.
     */
    private final HttpRequestInitializer initializer;

    /**
     * Create a new {@link HttpRequestFactoryProvider} with the optionally injected parameters.
     * <p />
     * Each parameter provides control over generated {@code HttpRequest}s.
     *
     * @param proxy The optional proxy to use for all HTTP Requests.
     * @param sslSocketFactory The optional SSL Socket factory for all HTTP Requests.
     * @param hostnameVerifier The optional hostname verifier for all HTTP Requests.
     * @param initializer The optional initializer that enables configuration for all HTTP Requests.
     */
    @Inject
    public HttpRequestFactoryProvider(@Nullable Proxy proxy,
                                      @Nullable SSLSocketFactory sslSocketFactory,
                                      @Nullable HostnameVerifier hostnameVerifier,
                                      @Nullable HttpRequestInitializer initializer) {
        // optional
        this.proxy = proxy;
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
        this.initializer = initializer;
    }

    /**
     * {@inheritDoc}
     */
    @Singleton
    @Override
    public HttpRequestFactory get() {
        return new NetHttpTransport.Builder()
                .setProxy(proxy)
                .setSslSocketFactory(sslSocketFactory)
                .setHostnameVerifier(hostnameVerifier)
            .build().createRequestFactory(initializer);
    }
}
