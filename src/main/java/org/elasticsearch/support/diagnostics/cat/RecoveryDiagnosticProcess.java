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
 * {@code RecoveryDiagnosticProcess} performs a request to <code>{baseUrl}/_recovery?detailed&pretty&human</code> and
 * writes to <code>"cat_recovery.txt"</code>.
 * <p />
 * The <code>_cat/recovery</code> API was not added until Elasticsearch 1.0.0, but <code>_recovery</code> is used in
 * favor to it starting with Elasticsearch 1.1.0.
 */
@Singleton
class RecoveryDiagnosticProcess extends VersionedHttpRequestDiagnosticProcess {
    /**
     * Create a new {@link org.elasticsearch.support.diagnostics.indices.RecoveryDiagnosticProcess}.
     */
    @Inject
    RecoveryDiagnosticProcess(HttpRequestFactory httpRequestFactory) {
        super("_cat/recovery?v", "cat_recovery.txt", httpRequestFactory,
              Version.VERSION_1_0, Version.VERSION_1_1);
    }
}
