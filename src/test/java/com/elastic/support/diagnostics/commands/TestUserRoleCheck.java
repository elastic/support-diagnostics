package com.elastic.support.diagnostics.commands;


import com.elastic.support.diagnostics.commands.UserRoleCheckCmd;
import com.elastic.support.util.JsonYamlUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestUserRoleCheck {

    JsonNode versionEarlierValidUser = JsonYamlUtils.createJsonNodeFromString("{\"elastic\": { \"username\": \"elastic\", \"roles\": [\"admin\"] }}");
    JsonNode versionEarlierInvalidUser = JsonYamlUtils.createJsonNodeFromString("{\"elastic\": { \"username\": \"elastic\", \"roles\": [\"notadmin\"] }}");
    JsonNode versionLaterValidUser = JsonYamlUtils.createJsonNodeFromString("{\"elastic\": { \"username\": \"elastic\", \"roles\": [\"superuser\"] }}");
    JsonNode versionLaterInvalidUser = JsonYamlUtils.createJsonNodeFromString("{\"elastic\": { \"username\": \"elastic\", \"roles\": [\"notsuperuser\"] }}");

    UserRoleCheckCmd cmd = new UserRoleCheckCmd();

    @Test
    public void testEarlierValidUser(){
        assertTrue(cmd.checkForAuth(2, "elastic", versionEarlierValidUser));
    }

    @Test
    public void testEarlierInvalidUser(){
        assertFalse(cmd.checkForAuth(2, "elastic", versionEarlierInvalidUser));
    }

    @Test
    public void testLaterValidUser(){
        assertTrue(cmd.checkForAuth(6, "elastic", versionLaterValidUser));
    }

    @Test
    public void testLaterInvalidUser(){
        assertFalse(cmd.checkForAuth(6, "elastic", versionLaterInvalidUser));
    }

}
