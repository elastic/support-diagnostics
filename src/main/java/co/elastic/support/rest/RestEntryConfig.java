/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.rest;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semver4j.Semver;

import java.util.LinkedHashMap;
import java.util.Map;

public class RestEntryConfig {

    private static final Logger logger = LogManager.getLogger(RestEntryConfig.class);

    private final Semver semver;
    private final String mode;

    public RestEntryConfig(String version) {
        this(version, "full");
    }

    public RestEntryConfig(String version, String mode) {
        this.semver = new Semver(version);
        this.mode = mode;
    }

    public Map<String, RestEntry> buildEntryMap(Map<String, Object> config) {
        Map<String, RestEntry> entries = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            final String name = entry.getKey();
            final RestEntry re = build(entry);

            if (re == null) {
                logger.warn("{} was bypassed due to mode", name);
            } else if (re.getUrl().equals(RestEntry.MISSING)) {
                logger.warn("{} was bypassed due to version check.", name);
            } else {
                entries.put(name, re);
            }
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    private RestEntry build(Map.Entry<String, Object> entry) {
        Map<String, Object> values = (Map<String, Object>) entry.getValue();

        // only some diagnostics provide a mode (currently only Elasticsearch)
        // currently "tags" is a simple string, but if we ever need it to be an
        // array, then naturally this will need to change
        if ("full".equals(mode) == false && mode.equals(values.get("tags")) == false) {
            return null;
        }

        return buildRestEntryForVersion(entry.getKey(), values);
    }

    @SuppressWarnings("unchecked")
    private RestEntry buildRestEntryForVersion(String name, Map<String, Object> entry) {
        String subdir = (String) ObjectUtils.defaultIfNull(entry.get("subdir"), "");
        String extension = (String) ObjectUtils.defaultIfNull(entry.get("extension"), ".json");
        boolean retry = (boolean) ObjectUtils.defaultIfNull(entry.get("retry"), false);
        boolean showErrors = (boolean) ObjectUtils.defaultIfNull(entry.get("showErrors"), true);

        Map<String, Object> versions = (Map<String, Object>) entry.get("versions");

        for (Map.Entry<String, Object> urlVersion : versions.entrySet()) {
            if (semver.satisfies(urlVersion.getKey())) {
                if (urlVersion.getValue() instanceof String) {
                    return new RestEntry(name, subdir, extension, retry, (String) urlVersion.getValue(), showErrors);
                    // We allow it to be String,String or String,Map(url,paginate,spaceaware)
                } else if (urlVersion.getValue() instanceof Map) {
                    Map<String, Object> info = (Map<String, Object>) urlVersion.getValue();

                    String url = (String) ObjectUtils.defaultIfNull(info.get("url"), null);

                    if (url == null) {
                        throw new RuntimeException("Undefined URL for REST entry (route)");
                    }

                    String pageableFieldName = (String) ObjectUtils.defaultIfNull(info.get("paginate"), null);
                    boolean spaceAware = (boolean) ObjectUtils.defaultIfNull(info.get("spaceaware"), false);

                    return new RestEntry(name, subdir, extension, retry, url, showErrors, pageableFieldName, spaceAware);
                }
            }
        }

        return null;
    }
}
