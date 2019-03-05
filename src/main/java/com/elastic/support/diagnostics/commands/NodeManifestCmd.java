package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.DiagnosticInputs;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestExec;
import com.elastic.support.util.JsonYamlUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class NodeManifestCmd implements Command {

    private final Logger logger = LogManager.getLogger(NodeManifestCmd.class);

    public void execute(DiagnosticContext context) {

        logger.info("Trying REST Endpoint.");
        DiagnosticInputs diagnosticInputs = context.getDiagnosticInputs();
        RestClient restClient = context.getEsRestClient();
        String result = restClient.execQuery("/_nodes/*/name,ip,host").toString();
        JsonNode root = JsonYamlUtils.createJsonNodeFromString(result);
        context.setNodeManifest(root);

    }


}
