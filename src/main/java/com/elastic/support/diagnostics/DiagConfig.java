package com.elastic.support.diagnostics;

import com.elastic.support.config.BaseConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class DiagConfig extends BaseConfig {

    private static Logger logger = LogManager.getLogger(DiagConfig.class);


    private Map<String, Integer> logSettings;
    private List<String> textFileExtensions;
    private int callRetries, pauseRetries;
    private List<String> requireRetry;
    private Map restCalls;

    public DiagConfig(){
        super();
    }

    public DiagConfig(Map configuration) {

        super(configuration);

        // Settings for REST calls to check for new diagnostic version

        // How many rolled over logs do we get?
        logSettings = (Map<String, Integer>) configuration.get("log-settings");

        // Differentiate between json and plain text results
        textFileExtensions = (List<String>) configuration.get("text-file-extensions");

        // REST settings for HttpClient

        // Whether we retry a failed call, how many times, and how long to wait before reattempting.
        requireRetry = (List<String>)configuration.get("require-retry");
        callRetries = (Integer) configuration.get("call-retries");
        pauseRetries = (Integer)configuration.get("pause-retries");

        // The REST calls we execute
        restCalls = (Map) configuration.get("rest-calls");


    }

    public Map<String, Integer> getLogSettings() {
        return logSettings;
    }

    public void setLogSettings(Map<String, Integer> logSettings) {
        this.logSettings = logSettings;
    }

    public List<String> getTextFileExtensions() {
        return textFileExtensions;
    }

    public void setTextFileExtensions(List<String> textFileExtensions) {
        this.textFileExtensions = textFileExtensions;
    }

    public int getCallRetries() {
        return callRetries;
    }

    public void setCallRetries(int callRetries) {
        this.callRetries = callRetries;
    }

    public int getPauseRetries() {
        return pauseRetries;
    }

    public void setPauseRetries(int pauseRetries) {
        this.pauseRetries = pauseRetries;
    }

    public List<String> getRequireRetry() {
        return requireRetry;
    }

    public void setRequireRetry(List<String> requireRetry) {
        this.requireRetry = requireRetry;
    }

    public Map getRestCalls() {
        return restCalls;
    }

    public void setRestCalls(Map restCalls) {
        this.restCalls = restCalls;
    }

    public Map<String, Map<String, String>> getSysCalls(String key){
        return (Map<String, Map<String, String>>)configuration.get(key);
    }

    public Map<String, String> getCommandMap(String key){
        return (Map<String, String>)configuration.get(key);
    }
}

