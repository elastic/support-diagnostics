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
package org.elasticsearch.support.diagnostics.preprocessor;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * {@code PreprocessorModule} provides {@link Preprocessor}s to be run before all {@code DiagnosticProcess}es.
 */
public class PreprocessorModule extends AbstractModule {
    /**
     * Configures a {@link Multibinder} to provide preprocessors.
     */
    @Override
    protected void configure() {
        Multibinder<Preprocessor> multibinder = Multibinder.newSetBinder(binder(), Preprocessor.class);

        // NOTE: These are ordered intentionally and their order _is_ maintained.

        // host:port
        multibinder.addBinding().to(HostPortPreprocessor.class);
        // output directory
        multibinder.addBinding().to(OutputDirectoryPreprocessor.class);
    }
}
