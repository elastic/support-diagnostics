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
package org.elasticsearch.support.diagnostics.settings;

import com.google.common.collect.ImmutableMap;

import org.elasticsearch.support.diagnostics.preprocessor.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * TODO: Document and figure out the best way to build this with the _current_ intent to have the running ES version
 * within it immutably.
 * <p />
 * Currently planning on a preprocessing step before running {@code DiagnosticProcess}es that would effectively build
 * an instance of {@code DiagnosticSettings}.
 */
public class DiagnosticSettings {
    /**
     * {@link DiagnosticSettings} logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticSettings.class);

    /**
     *
     */
    private final ImmutableMap<String, String> settings;
    private final Version version;

    public DiagnosticSettings(Map<String, String> settings) {
        this(settings, Version.fromString(settings.get(Names.VERSION)));
    }

    public DiagnosticSettings(Map<String, String> settings, Version version) {
        // required
        this.settings = ImmutableMap.copyOf(settings);
        this.version = checkNotNull(version, "version cannot be null");
    }

    /**
     * Get the targeted version of Elasticsearch.
     * <p />
     * This should be used to determine whether or not specific {@code DiagnosticProcess}es should be run.
     *
     * @return Never {@code null}.
     */
    public Version getVersion() {
        return version;
    }

    public String get(String name) {
        String value = settings.get(name);

        if (value == null) {
            throw new IllegalArgumentException(name + " does not have a value");
        }

        return value;
    }

    public String get(String name, String defaultValue) {
        String value = settings.get(name);

        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    public Boolean getAsBoolean(String name, Boolean defaultValue) {
        Boolean boolValue = defaultValue;
        String value = settings.get(name);

        if ("true".equalsIgnoreCase(value)) {
            boolValue = true;
        }
        else if ("false".equalsIgnoreCase(value)) {
            boolValue = false;
        }
        else if (value != null) {
            // note that the setting is being misused (set or read)
            LOGGER.warn("[{}] expected to be either 'true' or 'false'. Assuming [{}]", value, defaultValue);
        }

        return boolValue;
    }

    public Integer getAsInt(String name, Integer defaultValue) {
        Integer value = defaultValue;

        if (value != null) {
            try {
                value = Integer.valueOf(settings.get(name));
            }
            catch (NumberFormatException e) {
                // note that the setting is being misused (set or read)
                LOGGER.warn("[{}] expected to be an integer. Assuming [{}]", value, defaultValue, e);
            }
        }

        return value;
    }
}
