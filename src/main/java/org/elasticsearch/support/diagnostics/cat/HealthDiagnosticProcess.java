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
package org.elasticsearch.support.diagnostics.cat;

import com.google.api.client.http.HttpRequestFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.elasticsearch.support.diagnostics.http.VersionedHttpRequestDiagnosticProcess;
import org.elasticsearch.support.diagnostics.settings.Version;

/**
 * {@code HealthDiagnosticProcess} performs a request to <code>{baseUrl}/_cat/health?v</code> and writes to
 * <code>"cat_health.txt"</code>.
 * <p />
 * The <code>_cat/health</code> API was not added until Elasticsearch 1.0.0.
 */
@Singleton
class HealthDiagnosticProcess extends VersionedHttpRequestDiagnosticProcess {
    /**
     * Create a new {@link HealthDiagnosticProcess}.
     */
    @Inject
    HealthDiagnosticProcess(HttpRequestFactory httpRequestFactory) {
        super("_cat/health?v", "cat_health.txt", httpRequestFactory, Version.VERSION_1_0, null);
    }
}
