package com.elastic.support.monitoring;


import com.elastic.support.BaseConfig;
import com.elastic.support.Constants;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import com.vdurmont.semver4j.Semver;
import org.apache.commons.lang3.math.NumberUtils;
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

public class MonitoringImportConfig extends BaseConfig {

    protected int bulkSize ;

    public String monitoringExtractIndexPattern;
    public String logstashExtractIndexPattern;
    public String metricbeatExtractIndexPattern;

    public long bulkPause;
    public int  bulkMaxRetries;

    public List<String> templateList;

    Logger logger = LogManager.getLogger(MonitoringImportConfig.class);

    public MonitoringImportConfig(Map configuration) {
        super(configuration);
        bulkSize = (Integer) configuration.get("bulk-import-size");
        monitoringExtractIndexPattern = (String)configuration.get("monitoring-extract-pattern");
        logstashExtractIndexPattern = (String)configuration.get("logstash-extract-pattern");
        metricbeatExtractIndexPattern = (String)configuration.get("metricbeatExtractIndexPattern");
        templateList = (List<String>)configuration.get("import-templates");
        bulkPause = NumberUtils.createLong(configuration.get("bulk-pause").toString());
        bulkMaxRetries =  NumberUtils.createInteger(configuration.get("bulk-max-retries").toString());

    }


}

