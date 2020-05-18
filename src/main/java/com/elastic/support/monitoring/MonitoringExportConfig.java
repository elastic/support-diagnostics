package com.elastic.support.monitoring;


import com.elastic.support.BaseConfig;
import com.elastic.support.Constants;
import com.elastic.support.util.SystemProperties;
import com.vdurmont.semver4j.Semver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MonitoringExportConfig extends BaseConfig {

    protected List<String> monitoringStats = new ArrayList<>();
    protected Map<String, String> queries = new LinkedHashMap<>();
    protected String monitoringStartUri;
    protected String monitoringUri;
    protected String monitoringScrollUri;
    protected String monitoringScrollTtl;
    protected int  monitoringScrollSize;
    protected int bulkSize ;
    protected String bulkUri;
    protected String bulkIndexLine;
    protected Semver semver;

    Logger logger = LogManager.getLogger(MonitoringExportConfig.class);

    public MonitoringExportConfig(Map configuration) {
        super(configuration);
        monitoringStats = (List<String>) configuration.get("monitoring-stats");
        monitoringScrollSize = (Integer)configuration.get("monitoring-scroll-size");
        Map<String, String> queryFiles =(Map<String, String>) configuration.get("monitoring-queryfiles");
        queries = getQueriesFromFiles(queryFiles);

    }

    public void setVersion(Semver semver){
        this.semver = semver;
        monitoringUri = getVersionedQuery("monitoring-uri");
        monitoringStartUri = getVersionedQuery("monitoring-start-scroll-uri");
        monitoringScrollUri = getVersionedQuery("monitoring-scroll-uri");
    }

    public String getMonitoringUri() {
        return monitoringUri;
    }

    public String getMonitoringScrollUri() {
        return monitoringScrollUri;
    }

    public String getMonitoringScrollTtl() {
        return monitoringScrollTtl;
    }

    public int getMonitoringScrollSize() {
        return monitoringScrollSize;
    }

    public List<String> getMonitoringStats() {
        return monitoringStats;
    }

    public Map<String, String> getQueries() {
        return queries;
    }

    private Map<String, String> getQueriesFromFiles(Map<String, String> queryFiles) {

        Map<String, String> buildQueries = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : queryFiles.entrySet()) {
            StringBuilder resultStringBuilder = new StringBuilder();
            String  path = Constants.QUERY_CONFIG_PACKAGE + entry.getValue();
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);

            try {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        resultStringBuilder.append(line).append(SystemProperties.lineSeparator);
                    }
                }
            } catch (IOException e) {
                logger.info(Constants.CONSOLE, "Failed to read query configuration file");
                logger.info("Bad config", e);
            }

            buildQueries.put(entry.getKey(), resultStringBuilder.toString());

        }

        return buildQueries;

    }

    private String getVersionedQuery(String query ){
        Map<String, Map> urls  = (Map<String, Map>) configuration.get("monitoring-queries");
        Map<String, Map<String, String>> querySet = urls.get(query);
        Map<String, String> versions = querySet.get("versions");

        for(Map.Entry<String, String> urlVersion: versions.entrySet()){
            if(semver.satisfies(urlVersion.getKey())){
                return urlVersion.getValue();
            }
        }

        return "";

    }

}

