/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.util.UrlUtils;
import co.elastic.support.Constants;
import co.elastic.support.diagnostics.chain.Command;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.rest.RestClient;
import co.elastic.support.rest.RestEntry;
import co.elastic.support.rest.RestResult;
import co.elastic.support.util.JsonYamlUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.semver4j.Semver;

import java.util.List;
import java.util.Map;

public class CheckUserAuthLevel implements Command {
    @Override
    public void execute(DiagnosticContext context) {
        final String inputUsername = context.diagnosticInputs.user;

        // No user, it's not secured so no auth level or built-in admin role.
        if (StringUtils.isEmpty(inputUsername) || "elastic".equals(inputUsername)) {
            return;
        }

        // Unlike most APIs, the username is passed as a part of the URL and
        // thus it needs to be URL-encoded for the rare instance where special
        // characters are used
        String username = UrlUtils.encodeValue(inputUsername);

        // Should already be there.
        RestClient restClient = context.resourceCache.getRestClient(Constants.restInputHost);

        boolean hasAuthorization = false;
        Semver version = context.version;
        Map<String, RestEntry> calls = context.fullElasticRestCalls;
        RestEntry entry = calls.get("security_users");
        String url = entry.getUrl() + "/" + username;

        RestResult result = restClient.execQuery(url);

        if (result.getStatus() == 200) {
            String userJsonString = result.toString();
            JsonNode userNode = JsonYamlUtils.createJsonNodeFromString(userJsonString);
            hasAuthorization = checkForAuth(version.getMajor(), context.diagnosticInputs.user, userNode);
        }

        context.isAuthorized = hasAuthorization;
    }

    public boolean checkForAuth(int major, String user, JsonNode userNode) {
        JsonNode rolesNode = userNode.path(user).path("roles");
        boolean hasAuthorization = false;

        if (rolesNode.isArray()) {
            ObjectMapper mapper = new ObjectMapper();
            List<?> roles = mapper.convertValue(rolesNode, List.class);

            if (major <= 2) {
                hasAuthorization = roles.contains("admin");
            } else {
                hasAuthorization = roles.contains("superuser");
            }
        }

        return hasAuthorization;
    }
}
