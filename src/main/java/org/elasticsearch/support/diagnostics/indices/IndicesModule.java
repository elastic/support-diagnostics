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

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import org.elasticsearch.support.diagnostics.DiagnosticProcess;

/**
 * {@code IndicesModule} provides {@link DiagnosticProcess}es associated with indices.
 */
public class IndicesModule extends AbstractModule {
    /**
     * Configures a {@link Multibinder} to provide index related diagnostics.
     */
    @Override
    protected void configure() {
        Multibinder<DiagnosticProcess> multibinder = Multibinder.newSetBinder(binder(), DiagnosticProcess.class);

	// _count
	multibinder.addBinding().to(CountDiagnosticProcess.class);
        // _mapping
        multibinder.addBinding().to(MappingDiagnosticProcess.class);
        // _recovery?detailed
        multibinder.addBinding().to(RecoveryDiagnosticProcess.class);
        // _settings
        multibinder.addBinding().to(SettingsDiagnosticProcess.class);
        // _segments
        multibinder.addBinding().to(SegmentsDiagnosticProcess.class);
        // _stats
        multibinder.addBinding().to(StatsDiagnosticProcess.class);
        // _upgrade
        multibinder.addBinding().to(UpgradeDiagnosticProcess.class);
    }
}
