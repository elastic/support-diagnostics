/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.rest;

import lombok.Getter;
import java.util.Map;

@Getter
public class RestEntry {
    public static final String MISSING = "missing";

    private final String name;
    private final String url;
    private final String subdir;
    private final String extension;
    private final boolean retry;
    private final boolean showErrors;
    private final String pageableFieldName;
    private final boolean pageable;
    private final boolean spaceAware;
    private Map<String, String> extraHeaders;

    public RestEntry(String name, String subdir, String extension, boolean retry, String url, boolean showErrors) {
        this(name, subdir, extension, retry, url, showErrors, null, false);
    }

    public RestEntry(
        String name,
        String subdir,
        String extension,
        boolean retry,
        String url,
        boolean showErrors,
        String pageableFieldName,
        boolean spaceAware
    ) {
        this.name = name;
        this.subdir = subdir;
        this.extension = extension;
        this.retry = retry;
        this.url = url;
        this.showErrors = showErrors;
        this.pageableFieldName = pageableFieldName;
        this.pageable = pageableFieldName != null;
        this.spaceAware = spaceAware;
    }

    public Map<String, String> getExtraHeaders() {
        return extraHeaders;
    }

    public void setExtraHeaders(Map<String, String> extraHeaders) {
        this.extraHeaders = extraHeaders;
    }

    public RestEntry copyWithNewUrl(String url, String subdir) {
        return new RestEntry(name, subdir, extension, retry, url, showErrors, pageableFieldName, spaceAware);
    }
}
