package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.DiagnosticContext;
import com.elastic.support.diagnostics.RestModule;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;


public class VersionAndClusterNameCheckCmd extends AbstractDiagnosticCmd{

    public boolean execute(DiagnosticContext context){

        // Get the version number and cluster name fromt the JSON returned
        // by just submitting the host/port combo
        Map resultMap = null;
        String url = context.getInputParams().getUrl();
        boolean rc = true;

       logger.info("Trying REST Endpoint.");

        try {
            RestModule restModule = context.getRestModule();
            String result = restModule.submitRequest(url);
            ObjectMapper mapper = new ObjectMapper();
            resultMap = mapper.readValue(result, LinkedHashMap.class);
            String clusterName = (String) resultMap.get("cluster_name");
            Map ver = (Map)resultMap.get("version");
            String versionNumber = (String)ver.get("number");
            context.setClusterName(clusterName);
            context.setVersion(versionNumber);

        } catch (Exception e) {
            logger.error("Error retrieving Elasticsearch version  - unable to continue..  Please make sure the proper connection parameters were specified", e);
            rc = false;
        }

        return rc;
    }


}
