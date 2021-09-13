/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.monitoring;


import co.elastic.support.BaseConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class MonitoringImportConfig extends BaseConfig {

    protected int bulkSize ;

    public String monitoringExtractIndexPattern;
    public String logstashExtractIndexPattern;
    public String metricbeatExtractIndexPattern;

    public String metricbeatTemplate;
    public String logstashTemplate;
    public String esTemplate;

    public List<String> templateList;

    Logger logger = LogManager.getLogger(MonitoringImportConfig.class);

    public MonitoringImportConfig(Map configuration) {
        super(configuration);
        bulkSize = (Integer) configuration.get("bulk-import-size");
        monitoringExtractIndexPattern = (String)configuration.get("monitoring-extract-pattern");
        logstashExtractIndexPattern = (String)configuration.get("logstash-extract-pattern");
        metricbeatExtractIndexPattern = (String)configuration.get("metricbeatExtractIndexPattern");
        templateList = (List<String>)configuration.get("import-templates");

    }


}

