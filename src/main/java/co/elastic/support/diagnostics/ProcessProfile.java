/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.diagnostics;

import java.util.HashSet;
import java.util.Set;

public class ProcessProfile {
    public boolean isHttp = false;
    public boolean isDocker = false;
    public boolean currentMaster = false;
    public String name = "";
    public String id = "";
    public String pid = "";
    public String jvmPid = "";
    public String logDir = "";
    public String networkHost;
    public String host;
    public String ip;
    public String httpPublishAddr = "";
    public int httpPort;
    public String os;
    public Set<String> boundAddresses = new HashSet<>();
    public JavaPlatform javaPlatform;


    public boolean equals(Object obj) {
        if (!(obj instanceof ProcessProfile)) {
            return false;
        }
        ProcessProfile input = (ProcessProfile) obj;
        if (input.id.equals(id)) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ProcessProfile{" +
                "isHttp=" + isHttp +
                ", isDocker=" + isDocker +
                ", currentMaster=" + currentMaster +
                ", name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", pid='" + pid + '\'' +
                ", jvmPid='" + jvmPid + '\'' +
                ", logDir='" + logDir + '\'' +
                ", networkHost='" + networkHost + '\'' +
                ", host='" + host + '\'' +
                ", ip='" + ip + '\'' +
                ", httpPublishAddr='" + httpPublishAddr + '\'' +
                ", httpPort=" + httpPort +
                ", os='" + os + '\'' +
                ", boundAddresses=" + boundAddresses +
                ", javaPlatform=" + javaPlatform +
                '}';
    }
}
