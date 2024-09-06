/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.rest;

import co.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RestEntry {

    private static final Logger logger = LogManager.getLogger(RestEntry.class);

    public static final String MISSING = "missing";

    public RestEntry(String name, String subdir, String extension, boolean retry, String url, boolean showErrors) {
        this.name = name;
        this.subdir = subdir;
        this.extension = extension;
        this.retry = retry;
        this.url = url;
        this.showErrors = showErrors;
    }

    // Copy constructor
    public RestEntry(RestEntry other) {
        this.name = other.name;
        this.subdir = other.subdir;
        this.extension = other.extension;
        this.retry = other.retry;
        this.url = other.url;
        this.showErrors = other.showErrors;
        this.isPageable = other.isPageable;
        this.pageableFieldName = other.pageableFieldName;
        this.isSpaceAware = other.isSpaceAware;
    }

    public String name;

    public String getName() {
        return name;
    }

    public String  url;

    public String getUrl() {
        return url;
    }

    public String subdir = SystemProperties.fileSeparator;

    public String getSubdir() {
        return subdir;
    }

    public String extension = "json";

    public String getExtension() {
        return extension;
    }

    public boolean retry = false;

    public boolean isRetry() {
        return retry;
    }

    public boolean showErrors = true;

    private String pageableFieldName = null;

    private boolean isPageable = false;

    private boolean isSpaceAware = false;

    public boolean isSpaceAware() {
        return isSpaceAware;
    }

    public boolean isPageable() {
        return isPageable;
    }

    public String getPageableFieldName() {
        return pageableFieldName;
    }

    public void setSpaceAware(boolean isSpaceAware) {
        this.isSpaceAware = isSpaceAware;
    }

    public void setPageableFieldName(String pageableFieldName) {
        if(pageableFieldName != null) {
            this.pageableFieldName = pageableFieldName;
            this.isPageable = true;
        }
    }
}
