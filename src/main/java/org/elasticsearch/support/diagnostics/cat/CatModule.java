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

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import org.elasticsearch.support.diagnostics.DiagnosticProcess;

/**
 * {@code CatModule} provides {@link DiagnosticProcess}es associated with more human friendly, tabular APIs.
 */
public class CatModule extends AbstractModule {
    /**
     * Configures a {@link Multibinder} to provide _cat (human readable versus JSON) related diagnostics.
     */
    @Override
    protected void configure() {
        Multibinder<DiagnosticProcess> multibinder = Multibinder.newSetBinder(binder(), DiagnosticProcess.class);

        // _cat/aliases
        multibinder.addBinding().to(AliasDiagnosticProcess.class);
        // _cat/allocation
        multibinder.addBinding().to(AllocationDiagnosticProcess.class);
        // _cat/count
        multibinder.addBinding().to(CountDiagnosticProcess.class);
        // _cat/fielddata
        multibinder.addBinding().to(FielddataDiagnosticProcess.class);
        // _cat/indices
        multibinder.addBinding().to(IndexDiagnosticProcess.class);
        // _cat/master
        multibinder.addBinding().to(MasterDiagnosticProcess.class);
        // _cat/pending_tasks
        multibinder.addBinding().to(PendingTasksDiagnosticProcess.class);
        // _cat/plugin
        multibinder.addBinding().to(PluginDiagnosticProcess.class);
        // _cat/recovery
        multibinder.addBinding().to(RecoveryDiagnosticProcess.class);
        // _cat/shards
        multibinder.addBinding().to(ShardDiagnosticProcess.class);
        // _cat/thread_pool
        multibinder.addBinding().to(ThreadPoolDiagnosticProcess.class);
    }
}
