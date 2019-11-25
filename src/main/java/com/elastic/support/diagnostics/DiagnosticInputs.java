package com.elastic.support.diagnostics;

import com.beust.jcommander.Parameter;
import com.elastic.support.Constants;
import com.elastic.support.rest.ElasticRestClientInputs;
import com.elastic.support.util.SystemProperties;
import com.sun.xml.bind.v2.runtime.reflect.opt.Const;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class DiagnosticInputs extends ElasticRestClientInputs {

    private static Logger logger = LogManager.getLogger(DiagnosticInputs.class);

    public final static String[]
            DiagnosticTypeValues = {
            Constants.local,
            Constants.local,
            Constants.logstash,
            Constants.logstashApi,
            Constants.docker,
            Constants.dockerApi};

    protected ArrayList<String> diagnosticTypes = new ArrayList<>(Arrays.asList(DiagnosticTypeValues));

    @Parameter (names = {"--dockerId"}, description = "ID of the docker container Elasticsearch is running in.")
    protected String dockerId;

    @Parameter(names = {"--type"}, description = "Designates the type of service to run. Enter local, local-api, remote, remote-api, docker, remote-docker, logstash, remote-logstash. Required.")
    protected String diagType = "standard";
    public String getDiagType() {
        return diagType;
    }

    public void setDiagType(String diagType) {
        this.diagType = diagType;
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

    public boolean validate(){
        if(! super.validate()){
            return false;
        }

        if(! diagnosticTypes.contains(diagType)){
            logger.info("Invalid diagnostic type entered. Please specify one of the following: {}", StringUtils.join(DiagnosticTypeValues));
            return false;
        }

        if(diagType.equals(Constants.docker)){
            if(StringUtils.isEmpty(dockerId)){
                logger.info("When specifying a diagnostic type of Docker you must provide a docker id.");
                return false;
            }
        }

        return true;

    }


    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());

        sb.append("Diagnostic Type: " + this.diagType + Constants.TAB );
        sb.append("Docker Id: "  + this.dockerId + Constants.TAB );

        return sb.toString().trim();

    }
}
