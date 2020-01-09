package com.elastic.support.diagnostics;

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
}
