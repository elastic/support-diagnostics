package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.DiagnosticInputs;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
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
        DiagnosticInputs diagnosticInputs = GlobalContext.getDiagnosticInputs();
        RestExec restExec = GlobalContext.getRestExec();
        HttpHost httpHost = new HttpHost(diagnosticInputs.getHost(),
                diagnosticInputs.getPort(),
                diagnosticInputs.getScheme());
        String result = restExec.execSimpleDiagnosticQuery("/_nodes/*/name,ip,host", httpHost);
        JsonNode root = JsonYamlUtils.createJsonNodeFromString(result);
        GlobalContext.setNodeManifest(root);

    }


}
