/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.diagnostics;

import co.elastic.support.BaseConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class DiagConfig extends BaseConfig {

    private static Logger logger = LogManager.getLogger(DiagConfig.class);


    public int callRetries, pauseRetries, maxLogs, maxGcLogs;
    public DiagConfig(Map configuration) {

        super(configuration);

        // When we retry a failed call how many times, and how long to wait before reattempting.
        callRetries = (Integer) configuration.get("call-retries");
        pauseRetries = (Integer) configuration.get("pause-retries");

        // How many rolled over logs do we get?
        Map<String, Integer> logSettings = (Map<String, Integer>) configuration.get("log-settings");
        maxGcLogs = logSettings.get("maxGcLogs");
        maxLogs = logSettings.get("maxLogs");

    }

    public Map<String, Map<String, String>> getSysCalls(String key){
        return (Map<String, Map<String, String>>)configuration.get(key);
    }

}

