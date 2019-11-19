package com.elastic.support.rest;

import com.elastic.support.util.SystemProperties;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.vdurmont.semver4j.Semver;
import jdk.internal.jshell.tool.resources.version;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RestEntryBuilder {

    private static final Logger logger = LogManager.getLogger(RestEntryBuilder.class);

    Semver semver;
    public RestEntryBuilder(String version){
        semver = new Semver(version);
    }

    public RestEntry build(Map.Entry<String, Object> entry){

        String name = entry.getKey();
        Map<String, Object> values = (Map)entry.getValue();
        String subdir = (String) ObjectUtils.defaultIfNull(values.get("subdir"), "");
        String extension = (String)ObjectUtils.defaultIfNull(values.get("extension"), ".json");
        Boolean retry = (Boolean)ObjectUtils.defaultIfNull(values.get("retry"), false);
        Map<String, String> versions = (Map)values.get("versions");
        String url = getVersionSpecificUrl(versions);
        return new RestEntry(name, subdir, extension, retry, url);

    }

    public List<RestEntry> buildEntryList(Map<String, Object> config){

        ArrayList<RestEntry> entries = new ArrayList<>();
        for(Map.Entry<String, Object> entry: config.entrySet()){
            entries.add(build(entry));
        }

        return entries;

    }

    private String getVersionSpecificUrl(Map<String, String> versions){
        for(Map.Entry<String, String> urlVersion: versions.entrySet()){
            if(semver.satisfies(urlVersion.getKey())){
                return urlVersion.getValue();
            }
        }

        // Something is royally screwed up if you hit this but it's not a reason to start
        // tossing exceptions. Just log it and sub out the version url.
        logger.log(SystemProperties.DIAG, "Failed to obtain a url for these entries: {}", version::new);
        return "/";
    }

}
