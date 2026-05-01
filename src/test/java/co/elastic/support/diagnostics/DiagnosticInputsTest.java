/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics;

import co.elastic.support.Constants;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticInputsTest {

    @Test
    void defaultPort_isElasticsearch() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        assertEquals(9200, inputs.port);
    }

    @Test
    void setDefaultPort_logstash() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        inputs.setDefaultPortForDiagType(Constants.logstashApi);
        assertEquals(Constants.LOGSTASH_PORT, inputs.port);
    }

    @Test
    void setDefaultPort_kibana() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        inputs.setDefaultPortForDiagType(Constants.kibanaApi);
        assertEquals(Constants.KIBANA_PORT, inputs.port);
    }

    @Test
    void setDefaultPort_doesNotOverrideNonDefault() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        inputs.port = 19200;
        inputs.setDefaultPortForDiagType(Constants.logstashApi);
        assertEquals(19200, inputs.port, "Custom port should not be overridden");
    }

    @Test
    void validateDiagType_valid() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        for (String type : DiagnosticInputs.diagnosticTypeValues) {
            List<String> errors = inputs.validateDiagType(type);
            assertNull(errors, "Expected no errors for type: " + type);
        }
    }

    @Test
    void validateDiagType_invalid() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        List<String> errors = inputs.validateDiagType("not-a-real-type");
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void validateMode_lightAndFull() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        assertNull(inputs.validateMode("light"));
        assertNull(inputs.validateMode("full"));
    }

    @Test
    void validateMode_invalid() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        List<String> errors = inputs.validateMode("medium");
        assertNotNull(errors);
        assertEquals(1, errors.size());
    }

    @Test
    void validateRemoteUser_requiredForRemote() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        inputs.diagType = Constants.remote;
        List<String> errors = inputs.validateRemoteUser("");
        assertNotNull(errors);
        assertTrue(errors.size() > 0);
    }

    @Test
    void validateRemoteUser_notRequiredForApi() {
        DiagnosticInputs inputs = new DiagnosticInputs();
        inputs.diagType = Constants.api;
        assertNull(inputs.validateRemoteUser(""));
    }
}
