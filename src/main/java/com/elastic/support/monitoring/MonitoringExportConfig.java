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
import java.util.*;

public class MonitoringExportConfig extends BaseConfig {

    protected List<String> monitoringStats = new ArrayList<>();
    protected Map<String, String> queries = new LinkedHashMap<>();
    protected String monitoringStartUri;
    protected String monitoringUri;
    protected String monitoringScrollUri;
    protected String monitoringScrollTtl;
    protected int  monitoringScrollSize;
    protected Semver semver;

    protected List<String> queryFiles;
    protected List<String> monitorSets;
    protected List<String> logstashSets;
    protected List<String> metricSets;

    Logger logger = LogManager.getLogger(MonitoringExportConfig.class);

    public MonitoringExportConfig(Map configuration) {

        super(configuration);

        monitorSets = (List<String>) configuration.get("monitor-sets");
        metricSets = (List<String>) configuration.get("metric-sets");
        logstashSets = (List<String>) configuration.get("logstash-sets");
        queryFiles = (List<String>) configuration.get("query-files");
        monitoringScrollSize = (Integer)configuration.get("monitoring-scroll-size");
        queries = getQueriesFromFiles();

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

    public List<String> getStatsByType(String type){

        List<String> stats = new ArrayList<>();
        switch (type){
            case "all" :
                stats.addAll(monitorSets);
                stats.addAll(logstashSets);
                stats.addAll(metricSets);
                return stats;
            case "metric" :
                return metricSets;
            default:
                stats.addAll(monitorSets);
                stats.addAll(logstashSets);
                return stats;
        }
    }

    private Map<String, String> getQueriesFromFiles() {

        Map<String, String> buildQueries = new LinkedHashMap<>();
        for (String entry: queryFiles) {
            StringBuilder resultStringBuilder = new StringBuilder();
            String  path = Constants.QUERY_CONFIG_PACKAGE + entry + ".json";
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

            buildQueries.put(entry, resultStringBuilder.toString());

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

