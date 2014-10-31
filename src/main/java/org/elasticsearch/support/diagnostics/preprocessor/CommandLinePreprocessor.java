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

import org.elasticsearch.support.diagnostics.DiagnosticToolArgs;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code CommandLinePreprocessor} is an {@code abstract} basis for other {@link Preprocessor}s to extend to easily
 * contain it.
 */
public abstract class CommandLinePreprocessor implements Preprocessor {
    /**
     * The {@link DiagnosticToolArgs} that represent the command line arguments (and their defaults).
     */
    protected final DiagnosticToolArgs args;

    /**
     * Create a new {@link CommandLinePreprocessor}.
     *
     * @param args Parsed command line arguments.
     * @throws NullPointerException if {@code args} are {@code null}
     */
    public CommandLinePreprocessor(DiagnosticToolArgs args) {
        // required
        this.args = checkNotNull(args, "args cannot be null");
    }

    /**
     * Get the command line arguments.
     *
     * @return Never {@code null}.
     */
    public DiagnosticToolArgs getArgs() {
        return args;
    }
}
