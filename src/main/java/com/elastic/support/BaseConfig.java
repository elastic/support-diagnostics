package com.elastic.support;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class BaseConfig {

    public String diagnosticVersion;
    public int connectionTimeout;
    public int connectionRequestTimeout;
    public int socketTimeout;
    public int maxTotalConn;
    public int maxConnPerRoute;

    public String diagReleaseHost = "api.github.com";
    public String diagReleaseDest = "/repos/elastic/support-diagnostics/releases/latest";
    public String diagReleaseScheme = "https";
    public String diagLatestRelease = "https://api.github.com/repos/elastic/support-diagnostics/releases/latest";

    public Map<String, String> dockerGlobal;
    public Map<String, String> dockerContainer;

    protected Map configuration;

    public BaseConfig(Map configuration) {

        this.configuration = configuration;

        diagnosticVersion = (String)configuration.get("diagnosticVersion");

        Map<String, String> githubSettings = (Map<String, String>) configuration.get("github-settings");

        diagReleaseHost = githubSettings.get("diagReleaseHost");
        diagReleaseDest = githubSettings.get("diagReleaseDest");
        diagReleaseScheme = githubSettings.get("diagReleaseScheme");
        diagLatestRelease = githubSettings.get("diagLatestRelease");

        Map<String, Integer> restConfig = (Map<String, Integer>) configuration.get("rest-config");

        connectionTimeout = restConfig.get("connectTimeout") * 1000;
        connectionRequestTimeout = restConfig.get("requestTimeout") * 1000;
        socketTimeout = restConfig.get("socketTimeout") * 1000;
        maxTotalConn = restConfig.get("maxTotalConn");
        maxConnPerRoute = restConfig.get("maxConnPerRoute");

        dockerGlobal = (Map<String, String>)configuration.get("docker-global");
        dockerContainer = (Map<String, String> )configuration.get("docker-container");

    }

}
