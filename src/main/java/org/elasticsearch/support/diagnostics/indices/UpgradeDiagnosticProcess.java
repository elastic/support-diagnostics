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
package org.elasticsearch.support.diagnostics.indices;

import com.google.api.client.http.HttpRequestFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.elasticsearch.support.diagnostics.http.VersionedHttpRequestDiagnosticProcess;
import org.elasticsearch.support.diagnostics.settings.Version;

/**
 * {@code UpgradeDiagnosticProcess} performs a request to <code>{baseUrl}/_upgrade?pretty&human</code> and writes to
 * <code>"upgrade.json"</code>.
 * <p />
 * The <code>_upgrade</code> API was not added until Elasticsearch 1.4.0.
 */
@Singleton
class UpgradeDiagnosticProcess extends VersionedHttpRequestDiagnosticProcess {
    /**
     * Create a new {@link UpgradeDiagnosticProcess}.
     */
    @Inject
    UpgradeDiagnosticProcess(HttpRequestFactory httpRequestFactory) {
        super("_upgrade?pretty&human", "upgrade.json", httpRequestFactory, Version.VERSION_1_4, null);
    }
}
