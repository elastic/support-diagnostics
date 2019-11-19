package com.elastic.support.rest;

import com.elastic.support.util.SystemProperties;
import com.vdurmont.semver4j.Semver;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RestEntryFactory {

    private static final Logger logger = LogManager.getLogger(RestEntryFactory.class);

    Semver semver;
    public RestEntryFactory(String version){
        semver = new Semver(version, Semver.SemverType.NPM);
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
            RestEntry re = build(entry);
            if(re.getUrl().equals(RestEntry.MISSING) ){
                logger.log(SystemProperties.DIAG, "{} was bypassed due by version check.", re.getName());
            }
            else{
                entries.add(re);
            }
        }

        return entries;

    }

    private String getVersionSpecificUrl(Map<String, String> versions){
        for(Map.Entry<String, String> urlVersion: versions.entrySet()){
            if(semver.satisfies(urlVersion.getKey())){
                return urlVersion.getValue();
            }
        }

        // This can happen if it's older
        return RestEntry.MISSING;
    }

}
