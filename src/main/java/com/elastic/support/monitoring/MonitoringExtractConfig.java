package com.elastic.support.monitoring;


import com.elastic.support.config.BaseConfig;
import com.elastic.support.config.Constants;
import com.elastic.support.util.SystemProperties;
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

public class MonitoringExtractConfig extends BaseConfig {

    protected List<String> monitoringStats = new ArrayList<>();
    protected Map<String, String> queries = new LinkedHashMap<>();
    protected String monitoringUri;
    protected String monitoringScrollUri;
    protected String monitoringScrollTtl;
    protected int  monitoringScrollSize;
    protected int bulkSize ;
    protected String bulkUri;
    protected String bulkIndexLine;

    Logger logger = LogManager.getLogger(MonitoringExtractConfig.class);

    public MonitoringExtractConfig(Map configuration) {
        super(configuration);
        monitoringUri = (String)configuration.get("monitoring-uri");
        monitoringScrollUri = (String)configuration.get("monitoring-scroll-uri");
        monitoringScrollSize = (Integer)configuration.get("monitoring-scroll-size");
        monitoringScrollTtl = (String)configuration.get("monitoring-scroll-ttl");
        monitoringStats = (List<String>) configuration.get("monitoring-stats");
        monitoringScrollSize = (Integer) configuration.get("monitoring-scroll-size");

        bulkSize = (Integer) configuration.get("bulk-import-size");
        bulkUri = (String)configuration.get("bulk-uri");
        bulkIndexLine = (String)configuration.get("bulk-index-line");

        Map<String, String> queryFiles =(Map<String, String>) configuration.get("monitoring-queryfiles");
        queries = getQueriesFromFiles(queryFiles);
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
                logger.error("Failed to read query configuration file", e);
            }

            buildQueries.put(entry.getKey(), resultStringBuilder.toString());

        }

        return buildQueries;

    }

}

