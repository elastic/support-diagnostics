package com.elastic.support.diagnostics;

import com.beust.jcommander.Parameter;
import com.elastic.support.config.BaseInputs;
import com.elastic.support.config.Constants;
import com.elastic.support.config.ElasticClientInputs;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiagnosticInputs extends ElasticClientInputs {

    private static Logger logger = LogManager.getLogger(DiagnosticInputs.class);


    @Parameter(names = {"--accessLogs"}, description = "Use this option to collect access logs as well.")
    protected boolean accessLogs = false;
    @Parameter (names = {"--dockerId"}, description = "ID of the docker container Elasticsearch is running in.")
    protected String dockerId;

    @Parameter(names = {"--type"}, description = "DiagnosticService type to run. Enter standard, remote, logstash. Default is standard. Using remote will suppress retrieval of logs, configuration and system command info.")
    protected String diagType = "standard";
    public String getDiagType() {
        return diagType;
    }

    public void setDiagType(String diagType) {
        this.diagType = diagType;
    }

    public boolean isAccessLogs() {
        return accessLogs;
    }

    public void setAccessLogs(boolean accessLogs) {
        this.accessLogs = accessLogs;
    }

    public String getDockerId() {
        return dockerId;
    }

    public void setDockerId(String dockerId) {
        this.dockerId = dockerId;
    }

    public int getPort() {
        if (diagType.equalsIgnoreCase("logstash")) {
            if (port == 9200) {
                return Constants.LOGSTASH_PORT;
            }
        }
        return port;
    }




    @Override
    public String toString() {
        return "DiagnosticInputs{" +
                ", outputDir='" + outputDir + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", isSsl=" + isSsl +
                ", diagType='" + diagType + '\'' +
                ", skipVerification=" + skipVerification +
                ", skipAccessLogs=" + accessLogs +
                ", skip DiagVerify=" + bypassDiagVerify +
                '}';
    }
}
