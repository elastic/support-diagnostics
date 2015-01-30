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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.elasticsearch.support.diagnostics.DiagnosticToolArgs;
import org.elasticsearch.support.diagnostics.OperatingSystem;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 */
public class OutputDirectoryPreprocessor extends CommandLinePreprocessor {
    /**
     * The key associated with the output directory's path.
     */
    public static final String PATH_STRING = OutputDirectoryPreprocessor.class.getName() + ".path";
    /**
     * The key associated with a boolean flag indicating whether the directory was created by this process.
     */
    public static final String CREATED_BOOL = OutputDirectoryPreprocessor.class.getName() + ".created";

    /**
     * The timestamp format used by default to make diagnostic names unique (assuming they're not concurrently run).
     */
    public static final String TIMESTAMP_FORMAT = "yyyyMMddHHmmss";

    /**
     * The current operating system details.
     */
    private final OperatingSystem os;

    /**
     * Create a new {@link OutputDirectoryPreprocessor}.
     *
     * @param args Parsed command line arguments.
     * @param os The running operating system details
     * @throws NullPointerException if any parameter are {@code null}
     */
    @Inject
    OutputDirectoryPreprocessor(DiagnosticToolArgs args, OperatingSystem os) {
        super(args);

        // required
        this.os = checkNotNull(os, "os cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImmutableMap<String, String> run(ImmutableMap<String, String> settings) {
        // true if _we_ created the directory
        boolean created = true;

        Path path;

        // a directory was supplied
        if (args.getOutputDirectory() != null) {
            // we _could_ determine what directory existed already versus what we create
            created = false;

            path = Paths.get(args.getOutputDirectory());
        }
        // we need to default it to
        else {
            String hostname = os.getHostname();
            // target node without any problematic characters
            String nodeName = args.getNodeName().replaceAll("\\W+", "_");
            String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());

            path = Paths.get(".", String.format("support-diagnostics.%s.%s.%s", hostname, nodeName, timestamp));
        }

        return ImmutableMap.of(PATH_STRING, path.toString(), CREATED_BOOL, Boolean.toString(created));
    }
}
