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
package org.elasticsearch.support.diagnostics;

import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.inject.AbstractModule;
import com.google.inject.util.Providers;

import org.elasticsearch.support.diagnostics.http.HttpRequestFactoryProvider;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.net.Proxy;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The {@code DiagnosticsModule} provides the bindings for the {@code DiagnosticsTool} to use.
 */
public class DiagnosticsModule extends AbstractModule {
    /**
     * The command line arguments passed into the JVM.
     */
    private final DiagnosticToolArgs args;

    /**
     * Create a new {@link DiagnosticsModule} with the specified {@code args}.
     *
     * @param args User specified and defaulted command line arguments.
     * @throws NullPointerException if {@code args} is {@code null}
     */
    public DiagnosticsModule(DiagnosticToolArgs args) {
        // required
        this.args = checkNotNull(args, "args cannot be null");
    }

    /**
     * Configures all objects that are
     */
    @Override
    protected void configure() {
        // provide access to command line arguments; only preprocessors are expected to use this
        bind(DiagnosticToolArgs.class).toInstance(args);

        // placeholders for currently unsettable HttpRequestFactory configuration
        //  in the future, we can use Providers that depend on supplied settings to configure (or not) each instance
        bind(Proxy.class).toProvider(Providers.<Proxy>of(null));
        bind(SSLSocketFactory.class).toProvider(Providers.<SSLSocketFactory>of(null));
        bind(HostnameVerifier.class).toProvider(Providers.<HostnameVerifier>of(null));
        bind(HttpRequestInitializer.class).toProvider(Providers.<HttpRequestInitializer>of(null));

        bind(HttpRequestFactory.class).toProvider(HttpRequestFactoryProvider.class);

        // the DiagnosticsRunner combines all DiagnosticsProcess instances and runs them
        bind(DiagnosticsRunner.class);
    }
}
