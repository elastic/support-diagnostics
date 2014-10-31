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
package org.elasticsearch.support.diagnostics.process;

import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * {@code SynchronousProcessRunner} runs processes that are <em>already</em> synchronous. It does not synchronize
 * otherwise asynchronous processes.
 * <p />
 * This also assumes that the associated {@link Process} does not need to be told to close, but rather runs, prints
 * some UTF-8 (or just US-ASCII) text, and then closes on its own.
 */
public class SynchronousProcessRunner implements ProcessRunner {
    /**
     * {@link Logger} for {@link SynchronousProcessRunner}.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronousProcessRunner.class);

    /**
     * The process name passed to the {@link ProcessBuilder}. Must be the path to the process if it is not on the
     * <code>PATH</code>.
     */
    private final String processName;

    /**
     * Create a new {@link SynchronousProcessRunner} for a process with the {@code processName}.
     * <p />
     * Note: If the {@code processName} is <em>not</em> on the system's <code>PATH</code>, then it must include the
     * path to the executable file.
     *
     * @param processName The process name, or path and process name if it is not on the <code>PATH</code>
     * @throws NullPointerException if {@code processName} is {@code null}
     * @throws IllegalArgumentException if {@code processName} is blank
     */
    public SynchronousProcessRunner(String processName) {
        checkArgument( ! processName.trim().isEmpty(), "processName cannot be blank");

        // required
        this.processName = processName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String run(String... args) throws IOException {
        // show the process with its arguments
        LOGGER.info("Launching process [{}] with args: {}", processName, args);

        // put the process name as the first "argument" for the command to be run
        Process process = launchProcess(ImmutableList.<String>builder().add(processName).add(args).build());

        // output from the process
        StringBuilder processOutput = new StringBuilder();
        String output;

        LOGGER.trace("Attempting to read STDOUT from [{}]", processName);

        // attempt to read the STDOUT from the process
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;

            // read until EOF
            while ((line = reader.readLine()) != null) {
                processOutput.append(line).append('\n');
            }
        }

        output = processOutput.toString();

        // let the world know that we've escaped the process
        LOGGER.trace("Finished reading STDOUT from [{}]: {}", processName, output);

        // chop any extra whitespace (such as our trailing \n)
        return output.trim();
    }

    /**
     * Launch a new {@link Process} associated with the given {@code command}.
     * <p />
     * This method exists to enable simpler testing of the {@link #run} method as well as support for modifying
     * {@code Process}es before and after they are launched.
     *
     * @param command The command passed to the {@link ProcessBuilder}
     * @return Never {@code null}.
     * @throws IOException if any error occurs while launching the process
     */
    protected Process launchProcess(List<String> command) throws IOException {
        return new ProcessBuilder(command.toArray(new String[command.size()])).redirectErrorStream(true).start();
    }
}
