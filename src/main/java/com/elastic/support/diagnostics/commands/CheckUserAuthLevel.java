package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestEntry;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.ResourceCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdurmont.semver4j.Semver;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class CheckUserAuthLevel implements Command {

    Logger logger = LogManager.getLogger(CheckUserAuthLevel.class);

    @Override
    public void execute(DiagnosticContext context) {

        // No user, it's not secured so no auth level.
        if(StringUtils.isEmpty(context.diagnosticInputs.user) ){
            return;
        }

        // Should already be there.
        RestClient restClient = ResourceCache.getRestClient(Constants.restInputHost);

        boolean hasAuthorization = false;
        Semver version = context.version;
        Map<String, RestEntry> calls = context.elasticRestCalls;
        RestEntry entry =  calls.get("security_users");
        String url = entry.getUrl().replace("?pretty", "/" + context.diagnosticInputs.user);

        RestResult result = restClient.execQuery(url);

        if (result.getStatus() == 200) {
            String userJsonString = result.toString();
            JsonNode userNode = JsonYamlUtils.createJsonNodeFromString(userJsonString);
            hasAuthorization = checkForAuth(version.getMajor(), context.diagnosticInputs.user, userNode);
        }

        context.isAuthorized = hasAuthorization;

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
