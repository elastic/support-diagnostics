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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * {@code AbstractDiagnosticProcess} provides the common basis for all {@link DiagnosticProcess}es.
 */
public abstract class AbstractDiagnosticProcess implements DiagnosticProcess {
    /**
     * {@link AbstractDiagnosticProcess} logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDiagnosticProcess.class);

    /**
     * The name of the {@link DiagnosticProcess}.
     */
    private final String name;
    /**
     * The filename used to write the output.
     */
    private final String filename;

    /**
     * Create a new {@link AbstractDiagnosticProcess}.
     *
     * @param name The name of the process
     * @param filename The filename used to write the output
     * @throws NullPointerException if any parameter is {@code null}
     * @throws IllegalArgumentException if any parameter is blank
     */
    public AbstractDiagnosticProcess(String name, String filename) {
        checkArgument( ! name.trim().isEmpty(), "name cannot be blank");
        checkArgument( ! filename.trim().isEmpty(), "filename cannot be blank");

        // required
        this.name = name;
        this.filename = filename;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getFilename() {
        return filename;
    }

    /**
     * Logs the {@code throwable} exception and writes it to the {@code filepath}.error before returning a wrapper
     * {@link RuntimeException} for the {@code throwable}.
     *
     * @param filepath The file that would have been written too had the {@code throwable} exception not happened.
     * @param throwable The exception.
     * @return Never {@code null}.
     * @throws NullPointerException if {@code throwable} is {@code null}
     */
    protected RuntimeException rethrowableException(Path filepath, String message, Throwable throwable) {
        String errorFilename = filepath.toString() + ".error";

        // log the error incase anyone is looking
        LOGGER.error("[{}]: {}", errorFilename, message, throwable);

        // write the stacktrace to disk so that can be diagnosed if necessary
        try (PrintWriter writer = new PrintWriter(Paths.get(errorFilename).toFile())) {
            // use our own message as the header, which will hopefully give even more context than the filename
            writer.println(message);
            writer.println();

            // write the stacktrace to the file
            throwable.printStackTrace(writer);
        }
        catch (IOException e) {
            LOGGER.error("Unable to write [{}] to disk. Check permissions.", errorFilename, e);
        }

        return new RuntimeException(message, throwable);
    }
}
