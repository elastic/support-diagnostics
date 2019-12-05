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

public class MonitoringImportConfig extends BaseConfig {

    protected int bulkSize ;
    protected String bulkUri;
    protected String bulkIndexLine;

    Logger logger = LogManager.getLogger(MonitoringImportConfig.class);

    public MonitoringImportConfig(Map configuration) {
        super(configuration);
        bulkSize = (Integer) configuration.get("bulk-import-size");
        bulkUri = (String)configuration.get("bulk-uri");
        bulkIndexLine = (String)configuration.get("bulk-index-line");

    }


}

