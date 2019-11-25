package com.elastic.support;

import java.util.Map;

public class BaseConfig {

    protected Map configuration;
    protected Map<String, String> githubSettings;
    protected Map<String, Integer> restConfig;

    public BaseConfig() {
    }

    public BaseConfig(Map configuration) {
        this.configuration = configuration;
        githubSettings = (Map<String, String>) configuration.get("github-settings");
        restConfig = (Map<String, Integer>) configuration.get("rest-config");
    }

    public Map<String, String> getGithubSettings() {
        return githubSettings;
    }

    public void setGithubSettings(Map<String, String> githubSettings) {
        this.githubSettings = githubSettings;
    }

    public Map<String, Integer> getRestConfig() {
        return restConfig;
    }

    public void setRestConfig(Map<String, Integer> restConfig) {
        this.restConfig = restConfig;
    }
}
