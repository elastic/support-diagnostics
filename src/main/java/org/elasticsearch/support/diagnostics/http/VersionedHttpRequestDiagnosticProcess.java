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
package org.elasticsearch.support.diagnostics.http;

import com.google.api.client.http.HttpRequestFactory;

import org.elasticsearch.support.diagnostics.settings.DiagnosticSettings;
import org.elasticsearch.support.diagnostics.settings.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * {@code versionedHttpRequestDiagnosticProcess} extends {@link HttpRequestDiagnosticProcess} for any versioned
 * HTTP Request.
 * <p />
 * Extending this class instead of {@link UnversionedHttpRequestDiagnosticProcess} implies that the current diagnostic
 * does have version restrictions and therefore it will not always run.
 */
public abstract class VersionedHttpRequestDiagnosticProcess extends HttpRequestDiagnosticProcess {
    /**
     * {@link VersionedHttpRequestDiagnosticProcess} logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionedHttpRequestDiagnosticProcess.class);

    /**
     * The minimum (inclusive) version to require before running. This field is optional, which defaults to an unbounded
     * minimum version.
     */
    private final Version minVersion;
    /**
     * The maximum (exclusive) version to require before running. This field is optional, which defaults to an unbounded
     * maximum version.
     */
    private final Version maxVersion;

    /**
     * Create a new {@link VersionedHttpRequestDiagnosticProcess}.
     * <p />
     * The {@code encodedUrlSuffix} is used as the name without by stripping off any query string.
     *
     * @param encodedUrlSuffix The URL-encoded URL to append to the Elasticsearch base URL
     * @param filename The filename used to write the output
     * @param httpRequestFactory The HTTP Request factory for all requests
     * @param minVersion The minimum (inclusive) required release of Elasticsearch to support this diagnostic
     * @param maxVersion The maximum (exclusive) required release of Elasticsearch to support this diagnostic
     * @throws NullPointerException if any parameter is {@code null}, except {@code minVersion} or {@code maxVersion}
     * @throws IllegalArgumentException if any parameter is blank
     */
    public VersionedHttpRequestDiagnosticProcess(String encodedUrlSuffix,
                                                 String filename,
                                                 HttpRequestFactory httpRequestFactory,
                                                 @Nullable Version minVersion,
                                                 @Nullable Version maxVersion) {
        super(encodedUrlSuffix, filename, httpRequestFactory);

        // sanity check to ensure that maxVersion >= minVersion
        assert minVersion == null || maxVersion == null || maxVersion.isSameOrNewer(minVersion) : "Versions in wrong order";

        // optional
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
    }

    /**
     * Get the minimum allowed {@link Version} of Elasticsearch.
     * <p />
     * Note: This value is treated inclusively (greater than or equal to) rather than exclusively.
     *
     * @return Can be {@code null} to indicate that there is no minimum version.
     */
    public @Nullable Version getMinVersion() {
        return minVersion;
    }

    /**
     * Get the maximum allowed {@link Version} of Elasticsearch.
     * <p />
     * Note: This value is treated exclusively (less than, but not equal to) rather than inclusively.
     *
     * @return Can be {@code null} to indicate that there is no maximum version.
     */
    public @Nullable Version getMaxVersion() {
        return maxVersion;
    }

    /**
     * {@inheritDoc}
     * <p />
     * By default, this will use the {@link #getMinVersion() minimum} and {@link #getMaxVersion() maximum}
     * {@link Version}s to determine if the {@link DiagnosticSettings#getVersion() version} of Elasticsearch can be used
     * with this diagnostic.
     */
    @Override
    protected boolean isUsable(DiagnosticSettings settings) {
        // assume it's valid unless the version removes it
        boolean valid = true;
        // running version of Elasticsearch
        Version version = settings.getVersion();

        // the version is not new enough
        if (minVersion != null && ! version.isSameOrNewer(minVersion)) {
            LOGGER.debug("Skipping [{}] because {} is older than {}", getName(), version, minVersion);

            valid = false;
        }
        // the version is too new
        else if (maxVersion != null && version.isSameOrNewer(maxVersion)) {
            LOGGER.debug("Skipping [{}] because {} is at least {}", getName(), version, maxVersion);

            valid = false;
        }

        return valid;
    }
}
