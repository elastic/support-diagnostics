package com.elastic.support.config;

import com.elastic.support.util.JsonYamlUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class DiagConfig {

    private static Logger logger = LogManager.getLogger(DiagConfig.class);


    private Map<String, String> githubSettings;
    private Map<String, Integer> logSettings;
    private List<String> passwordKeys;
    private List<String> textFileExtensions;
    private Map<String, Integer> restConfig;
    private int callRetries, pauseRetries;
    private List<String> requireRetry;
    private Map restCalls;
    private Map configuration;


    public DiagConfig(){
        super();
    }

    public DiagConfig(Map configuration) {

        this.configuration = configuration;

        githubSettings = (Map<String, String>) configuration.get("github-settings");
        logSettings = (Map<String, Integer>) configuration.get("log-settings");
        passwordKeys = (List<String>) configuration.get("password-keys");
        textFileExtensions = (List<String>) configuration.get("text-file-extensions");
        restConfig = (Map<String, Integer>) configuration.get("rest-config");
        callRetries = (Integer) configuration.get("call-retries");
        pauseRetries = (Integer)configuration.get("pause-retries");
        requireRetry = (List<String>)configuration.get("require-retry");
        restCalls = (Map) configuration.get("rest-calls");

    }

    public Map<String, String> getGithubSettings() {
        return githubSettings;
    }

    public void setGithubSettings(Map<String, String> githubSettings) {
        this.githubSettings = githubSettings;
    }

    public Map<String, Integer> getLogSettings() {
        return logSettings;
    }

    public void setLogSettings(Map<String, Integer> logSettings) {
        this.logSettings = logSettings;
    }

    public List<String> getPasswordKeys() {
        return passwordKeys;
    }

    public void setPasswordKeys(List<String> passwordKeys) {
        this.passwordKeys = passwordKeys;
    }

    public List<String> getTextFileExtensions() {
        return textFileExtensions;
    }

    public void setTextFileExtensions(List<String> textFileExtensions) {
        this.textFileExtensions = textFileExtensions;
    }

    public Map<String, Integer> getRestConfig() {
        return restConfig;
    }

    public void setRestConfig(Map<String, Integer> restConfig) {
        this.restConfig = restConfig;
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

    public Map getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map configuration) {
        this.configuration = configuration;
    }

    public Map<String, String> getCommandMap(String key){
        return (Map<String, String>)configuration.get(key);
    }
}

