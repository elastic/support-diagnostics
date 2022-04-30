/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class BaseConfig {

    public String diagnosticVersion;
    public int connectionTimeout;
    public int connectionRequestTimeout;
    public int socketTimeout;
    public int maxTotalConn;
    public int maxConnPerRoute;
    public Map<String, String> extraHeaders;

    public String diagReleaseHost = "api.github.com";
    public String diagReleaseDest = "/repos/elastic/support-diagnostics/releases/latest";
    public String diagReleaseScheme = "https";
    public String diagLatestRelease = "https://api.github.com/repos/elastic/support-diagnostics/releases/latest";

    public Map<String, String> dockerGlobal;
    public Map<String, String> dockerContainer;
    public Map<String, String> kubernates;
    public String dockerContainerIds;
    public String dockerExecutablePath;

    protected Map configuration;

    public BaseConfig(Map configuration) {
        this.configuration = configuration;

        Map<String, String> githubSettings = (Map<String, String>) configuration.get("github-settings");

        if ( githubSettings != null){

            if (StringUtils.isNotEmpty(githubSettings.get("diagReleaseHost"))) {
                diagReleaseHost = githubSettings.get("diagReleaseHost");
            }

            if (StringUtils.isNotEmpty(githubSettings.get("diagReleaseDest"))) {
                diagReleaseDest = githubSettings.get("diagReleaseDest");
            }

            if (StringUtils.isNotEmpty(githubSettings.get("diagReleaseScheme"))) {
                diagReleaseScheme = githubSettings.get("diagReleaseScheme");
            }

            if (StringUtils.isNotEmpty(githubSettings.get("diagLatestRelease"))) {
                diagLatestRelease = githubSettings.get("diagLatestRelease");
            }
        }

        Map<String, Integer> restConfig = (Map<String, Integer>) configuration.get("rest-config");

        connectionTimeout = restConfig.get("connectTimeout") * 1000;
        connectionRequestTimeout = restConfig.get("requestTimeout") * 1000;
        socketTimeout = restConfig.get("socketTimeout") * 1000;
        maxTotalConn = restConfig.get("maxTotalConn");
        maxConnPerRoute = restConfig.get("maxConnPerRoute");

        extraHeaders = (Map<String, String>) configuration.get("extra-headers");

        dockerGlobal = (Map<String, String>) configuration.get("docker-global");

        dockerContainer = (Map<String, String>) configuration.get("docker-container");
        dockerContainerIds = (String) configuration.get("docker-container-ids");
        dockerExecutablePath = (String) configuration.get("docker-executable-location");
    }

}
