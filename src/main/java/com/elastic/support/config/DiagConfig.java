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
    private Map<String, Integer> callRetries;
    private Map restCalls;
    private Map configuration;


    public DiagConfig(Map configuratione) {

        this.configuration = configuratione;

        githubSettings = (Map<String, String>) configuration.get("github-settings");
        logSettings = (Map<String, Integer>) configuration.get("log-settings");
        passwordKeys = (List<String>) configuration.get("password-keys");
        textFileExtensions = (List<String>) configuration.get("text-file-extensions");
        restConfig = (Map<String, Integer>) configuration.get("rest-config");
        callRetries = (Map<String, Integer>) configuration.get("call-retries");
        restCalls = (Map) configuration.get("rest-calls");

    }

    public Map<String, String> getCommandMap(String name) {

        return (Map<String, String>) configuration.get(name);

    }

    public Map<String, String> getGithubSettings() {
        return githubSettings;
    }

    public Map<String, Integer> getLogSettings() {
        return logSettings;
    }

    public List<String> getPasswordKeys() {
        return passwordKeys;
    }

    public List<String> getTextFileExtensions() {
        return textFileExtensions;
    }

    public Map<String, Integer> getRestConfig() {
        return restConfig;
    }

    public Map<String, Integer> getCallRetries() {
        return callRetries;
    }

    public Map getRestCalls() {
        return restCalls;
    }
}
