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

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import org.elasticsearch.support.diagnostics.DiagnosticProcess;

/**
 * {@code ClusterModule} provides {@link DiagnosticProcess}es associated with clusters.
 */
public class ClusterModule extends AbstractModule {
    /**
     * Configures a {@link Multibinder} to provide cluster related diagnostics.
     */
    @Override
    protected void configure() {
        Multibinder<DiagnosticProcess> multibinder = Multibinder.newSetBinder(binder(), DiagnosticProcess.class);

        // _cluster/health
        multibinder.addBinding().to(HealthDiagnosticProcess.class);
        // _cluster/pending_tasks
        multibinder.addBinding().to(PendingTasksDiagnosticProcess.class);
        // _cluster/settings
        multibinder.addBinding().to(SettingsDiagnosticProcess.class);
        // _cluster/state
        multibinder.addBinding().to(StateDiagnosticProcess.class);
        // _cluster/stats
        multibinder.addBinding().to(StatsDiagnosticProcess.class);
        // _nodes
        multibinder.addBinding().to(NodeInfoDiagnosticProcess.class);
        // _nodes/stats
        multibinder.addBinding().to(NodeStatsDiagnosticProcess.class);
        // _nodes/hot_threads?type=cpu
        multibinder.addBinding().to(CpuHotThreadsDiagnosticProcess.class);
        // _nodes/hot_threads?type=wait
        multibinder.addBinding().to(WaitHotThreadsDiagnosticProcess.class);
        // _nodes/hot_threads?type=block
        multibinder.addBinding().to(BlockHotThreadsDiagnosticProcess.class);
        // [root]
        multibinder.addBinding().to(VersionDiagnosticProcess.class);
    }
}
