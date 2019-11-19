package com.elastic.support.rest;

import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class RestEntry {

    private static final Logger logger = LogManager.getLogger(RestEntry.class);

    public RestEntry(String name, String subdir, String extension, boolean retry, String url){
        this.name = name;
        this.subdir = subdir;
        this.extension = extension;
        this.retry = retry;
        this.url = url;
    }

    private String name;

    public String getName() {
        return name;
    }

    private String  url;

    public String getUrl() {
        return url;
    }

    private String subdir = SystemProperties.fileSeparator;

    public String getSubdir() {
        return subdir;
    }

    private String extension = "json";

    public String getExtension() {
        return extension;
    }

    private boolean retry = false;

    public boolean isRetry() {
        return retry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestEntry restEntry = (RestEntry) o;
        return name.equals(restEntry.name);
    }

}
