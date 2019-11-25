package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestEntry;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.JsonYamlUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdurmont.semver4j.Semver;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class RetrieveInitialNodeData implements Command {

    Logger logger = LogManager.getLogger(RetrieveInitialNodeData.class);

    @Override
    public void execute(DiagnosticContext context) {

        RestClient restClient = context.getEsRestClient();
        String user = context.getDiagnosticInputs().getUser();

        if(StringUtils.isEmpty(user) ){
            return;
        }

        boolean hasAuthorization = false;

        Semver version = context.getVersion();
        Map<String, RestEntry> calls = context.getElasticRestCalls();
        RestEntry entry =  calls.get("security_users");
        String url = entry.getUrl().replace("?pretty", "/" + user);

        RestResult result = restClient.execQuery(url);

        if (result.getStatus() == 200) {
            String userJsonString = result.toString();
            JsonNode userNode = JsonYamlUtils.createJsonNodeFromString(userJsonString);
            hasAuthorization = checkForAuth(version.getMajor(), user, userNode);
        }

        context.setAuthorized(hasAuthorization);

    }

    public boolean checkForAuth(int major, String user, JsonNode userNode){

        JsonNode rolesNode = userNode.path(user).path("roles");
        List<String> roles = null;
        boolean hasAuthorization = false;

        if (rolesNode.isArray()) {
            ObjectMapper mapper = new ObjectMapper();
            roles = mapper.convertValue(rolesNode, List.class);

            if (major <= 2) {
                hasAuthorization = roles.contains("admin");
            } else {
                hasAuthorization = roles.contains("superuser");
            }
        }

        return hasAuthorization;

    }
}
