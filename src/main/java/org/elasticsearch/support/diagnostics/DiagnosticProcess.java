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

import org.elasticsearch.support.diagnostics.settings.DiagnosticSettings;

/**
 * {@code DiagnosticProcess}es are effectively specific-purposed scripts that culminate in a single file's output. For
 * example, a {@code DiagnosticProcess} may exist to read the mappings of all indices (<code>_mapping</code>). This
 * feature only exists in Elasticsearch starting from version 1.0.
 * <pre>
 * MappingProcess mappingProcess = ...
 *
 * if (mappingProcess.run(config)) {
 *     //
 * }
 * </pre>
 * If the supplied version was earlier than 1.0 (e.g., 0.90.0), then the process should be skipped. Generically, this
 * should just be a loop of all {@code DiagnosticProcess}es.
 * <p />
 * In cases where an API changes between versions, the {@link #run} method should not add support for multiple versions.
 * Instead, it is expected that a different {@code DiagnosticProcess} would be written for the new behavior or API. By
 * doing it this way, multiple versions can be supported in the same codebase without much mystery and deprecation
 * becomes the matter of deleting old classes.
 */
public interface DiagnosticProcess {
    /**
     * Get the human readable name of the {@code DiagnosticProcess}. This value is expected to be logged or otherwise
     * displayed (e.g., stdout).
     * <p />
     * For example, a {@code DiagnosticProcess} that reads the text dump from <code>localhost:9200/_mapping</code>
     * might be named "Index Mappings".
     * <p />
     * For {@code DiagnosticProcess}es that are limited to a specific set of versions, then it should be reflected in
     * the name (e.g., "Fake Diagnostic 1.0+"). This way it should be obvious what caused it to be skipped in logs.
     *
     * @return Never blank. Always the same.
     */
    String getName();

    /**
     * Get the filename that will be used to write the diagnostics to disk.
     * <p />
     * For example, a {@code DiagnosticProcess} that reads the text dump from <code>localhost:9200/_mapping</code>
     * might write a file named <code>"mapping.json"</code>.
     * <p />
     * This intentionally excludes any non-constant parts. If it is always written into a subdirectory, then that
     * should be included (e.g., <code>"indices/mapping.json"</code>).
     * <p />
     * For prefixed files (e.g., <code>"logs/*"</code>), this should be provided as the prefix without special
     * characters (e.g., <code>"logs/"</code>). Doing so enables all filenames to be grouped for uniqueness should the
     * need or desire arise.
     *
     * @return Never blank. Always the same.
     */
    String getFilename();

    /**
     * Execute the {@code DiagnosticProcess} and write the output to {@link #getFilename()}.
     * <p />
     * It is both valid and expected that some {@code DiagnosticProcess}es will have no version requirements. For
     * example, reading <code>top</code> on Linux systems does not even consider Elasticsearch's version, but arguments
     * may vary (for instance, OS X uses different <code>top</code> arguments).
     * <p />
     * Note: Some {@code DiagnosticProcess}es are expected to have both a minimum <em>and</em> a maximum, while many
     * will only have a minimum (e.g., features that are <em>not</em> deprecated, but that came after 1.0).
     *
     * @param settings Setting/configuration parameters supplied by the user.
     * @return {@code true} if something was run. {@code false} if it was ignored because it did not match the current
     *         configuration.
     * @throws NullPointerException if {@code settings} is {@code null}
     * @throws RuntimeException if any unexpected error occurs while attempting to run the diagnostic (do not swallow
     *                          errors)
     */
    boolean run(DiagnosticSettings settings);
}
