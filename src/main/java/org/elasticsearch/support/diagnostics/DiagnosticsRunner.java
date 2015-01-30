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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.elasticsearch.support.diagnostics.preprocessor.OutputDirectoryPreprocessor;
import org.elasticsearch.support.diagnostics.preprocessor.Preprocessor;
import org.elasticsearch.support.diagnostics.settings.DiagnosticSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Set;

/**
 * The {@code DiagnosticsRunner} runs in three phases:
 * <ol>
 * <li>{@link Preprocessor}s</li>
 * <li>{@link DiagnosticProcess}es</li>
 * <li>{@link Postprocessor}s</li>
 * </ol>
 * These phases are expected to be run exactly once and {@link DiagnosticsRunner}s are therefore not intended to be
 * reused. To re-run a {@link DiagnosticsRunner}, just reconstruct it.
 */
@Singleton
public class DiagnosticsRunner {
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsRunner.class);

    /**
     *
     */
    private final ImmutableSet<Preprocessor> preprocesses;
    /**
     *
     */
    private final ImmutableSet<DiagnosticProcess> processes;

    /**
     *
     */
    private int exitCode = 0;

    /**
     * Create a new {@link DiagnosticsRunner} with the three phases.
     *
     * @param processes
     * @throws NullPointerException if any parameter is/contains {@code null}
     */
    @Inject
    public DiagnosticsRunner(Set<Preprocessor> preprocesses,
                             Set<DiagnosticProcess> processes) {
        // required
        this.preprocesses = ImmutableSet.copyOf(preprocesses);
        this.processes = ImmutableSet.copyOf(processes);
    }

    /**
     *
     *
     * @return <code>0</code> if all diagnostics run without error. Otherwise the index of the
     *         {@code DiagnosticProcess} + 1 that failed.
     */
    public int run() {
        int processIndex = 0;
        // if it remains 0, then the process has not failed
        int errorIndex = 0;

        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder().putAll(runPreprocessors());

        DiagnosticSettings settings = new DiagnosticSettings(builder.build());

        Paths.get(settings.get(OutputDirectoryPreprocessor.PATH_STRING)).toFile().mkdir();

        // currently synchronous, but this can likely be multithreaded in the future
        for (DiagnosticProcess process : processes) {
            LOGGER.debug("Running [{}] (index = {})...", process.getName(), processIndex++);

            try {
                // attempt to run the process
                if (process.run(settings)) {
                    LOGGER.info("Completed [{}] with '{}'", process.getName(), process.getFilename());
                }
                else {
                    LOGGER.debug("Skipped [{}]", process.getName());
                }
            }
            catch (RuntimeException e) {
                LOGGER.error("Unexpected error while running [{}]", process.getName(), e);

                // do not forget which process failed [first]
                if (errorIndex == 0) {
                    // all process errors start at 20000
                    errorIndex = 20000 + processIndex;
                }
            }
        }

        // if we didn't make it to the end, then the error code is the failed process' index + 1 (to avoid 0)
        return errorIndex;
    }

    /**
     *
     *
     * @return
     */
    protected ImmutableMap<String, String> runPreprocessors() {
        ImmutableMap.Builder<String, String> settings = ImmutableMap.builder();

        for (Preprocessor preprocessor : preprocesses) {
            LOGGER.debug("Running [{}]", preprocessor.getClass().getSimpleName());

            try {
                settings.putAll(preprocessor.run(settings.build()));
            }
            catch (IllegalStateException e) {
                LOGGER.error("Unexpected error while running [{}] stopping diagnostic",
                             preprocessor.getClass().getSimpleName(), e);
            }
            catch (RuntimeException e) {
                // Note: The full class name will be in the stacktrace
                LOGGER.error("Unexpected error while running [{}]", preprocessor.getClass().getSimpleName(), e);
            }
        }

        return settings.build();
    }
}
