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

    private RestEntry build(Map.Entry<String, Object> entry) {
        String name = entry.getKey();
        Map<String, Object> values = (Map) entry.getValue();

        // only some diagnostics provide a mode (currently only Elasticsearch)
        // currently "tags" is a simple string, but if we ever need it to be an
        // array, then naturally this will need to change
        if ("full".equals(mode) == false && mode.equals(values.get("tags")) == false) {
            return null;
        }
        
        RestEntry tmp = new RestEntry(
            name,
            (String) ObjectUtils.defaultIfNull(values.get("subdir"), ""),
            (String) ObjectUtils.defaultIfNull(values.get("extension"), ".json"),
            (Boolean) ObjectUtils.defaultIfNull(values.get("retry"), false),
            RestEntry.MISSING,
            (Boolean) ObjectUtils.defaultIfNull(values.get("showErrors"), true)
        );
        Map<String, Object> versions = (Map) values.get("versions");
        populateVersionSpecificUrlAndModifiers(versions, tmp);

        return tmp;
    }

    private void populateVersionSpecificUrlAndModifiers(Map<String, Object> versions, RestEntry entry) {
        for (Map.Entry<String, Object> urlVersion : versions.entrySet()) {
            if (semver.satisfies(urlVersion.getKey())) {
                // We allow it to be String,String or String,Map(url,paginate,spaceaware)
                if(urlVersion.getValue() instanceof Map) {
                    Map<String, Object> info = (Map) urlVersion.getValue();
                    entry.url = (String) ObjectUtils.defaultIfNull(info.get("url"), RestEntry.MISSING);
                    entry.setPageableFieldName((String)ObjectUtils.defaultIfNull(info.get("paginate"), null));
                    entry.setSpaceAware((boolean)ObjectUtils.defaultIfNull(info.get("spaceaware"), false));
                    return;
                } else if (urlVersion.getValue() instanceof String){
                    entry.url = (String) urlVersion.getValue();
                    return;
                }
            }
        }
    }

}
