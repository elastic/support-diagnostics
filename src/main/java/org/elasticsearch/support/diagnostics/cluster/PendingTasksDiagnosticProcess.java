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
package org.elasticsearch.support.diagnostics.cluster;

import com.google.api.client.http.HttpRequestFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.elasticsearch.support.diagnostics.http.VersionedHttpRequestDiagnosticProcess;
import org.elasticsearch.support.diagnostics.settings.Version;

/**
 * {@code PendingTasksDiagnosticProcess} performs a request to
 * <code>{baseUrl}/_cluster/pending_tasks?pretty&human</code> and writes to <code>"cluster_pending_tasks.json"</code>.
 * <p />
 * The <code>cluster/pending_tasks</code> API was not added until Elasticsearch 0.90.3.
 * <p />
 * It may be worth limiting this to versions prior to 1.x (added in 0.90.3) and supporting the
 * <code>_cat/pending_tasks</code> variant for versions later than 1.x. This is a bit more verbose, and it may be more
 * convenient to read the tabular format from _cat.
 */
@Singleton
class PendingTasksDiagnosticProcess extends VersionedHttpRequestDiagnosticProcess {
    /**
     * Create a new {@link PendingTasksDiagnosticProcess}.
     */
    @Inject
    PendingTasksDiagnosticProcess(HttpRequestFactory httpRequestFactory) {
        super("_cluster/pending_tasks?pretty&human", "cluster_pending_tasks.json", httpRequestFactory,
              Version.fromString("0.90.3"), null);
    }
}
