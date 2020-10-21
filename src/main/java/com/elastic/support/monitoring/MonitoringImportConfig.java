package com.elastic.support.monitoring;


import com.elastic.support.BaseConfig;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class MonitoringImportConfig extends BaseConfig {

    protected int bulkSize ;

    public String logstashPattern;
    public String metricbeatPattern;
    public String elasticPattern;

    public long bulkPause;
    public int  bulkMaxRetries;

    public List<String> templateList;

    Logger logger = LogManager.getLogger(MonitoringImportConfig.class);

    public MonitoringImportConfig(Map configuration) {
        super(configuration);
        bulkSize = (Integer) configuration.get("bulk-import-size");
        logstashPattern = (String)configuration.get("logstash-pattern");
        metricbeatPattern = (String)configuration.get("metricbeat-pattern");
        elasticPattern = (String)configuration.get("elastic-pattern");
        templateList = (List<String>)configuration.get("import-templates");
        bulkPause = NumberUtils.createLong(configuration.get("bulk-pause").toString());
        bulkMaxRetries =  NumberUtils.createInteger(configuration.get("bulk-max-retries").toString());

    }


}

