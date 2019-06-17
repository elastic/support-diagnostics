package com.elastic.support.config;

public class Version {

    String version;
    int major;
    int minor;
    int patch;

    public Version(String version){

        this.version = version;

        String[] ver = version.split("\\.");
        major = Integer.parseInt(ver[0]);
        minor = Integer.parseInt(ver[1]);

        if(ver.length > 2){
            patch = Integer.parseInt(ver[2]);
        }
        else {
            patch = -1;
        }
    }

    public String getVersion() {
        return version;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }
}
