/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.diagnostics.ProcessProfile;
import co.elastic.support.util.JsonYamlUtils;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KibanaGetDetailsTest {

    private final KibanaGetDetails details = new KibanaGetDetails();

    private JsonNode stats(String transportAddress) throws Exception {
        return JsonYamlUtils.createJsonNodeFromString(
                "{\"kibana\":{\"name\":\"kibana-1\",\"host\":\"kibana-1\",\"transport_address\":\""
                        + transportAddress + "\"},"
                        + "\"process\":{\"pid\":1234},\"os\":{\"platform\":\"linux\"}}");
    }

    @Test
    void ipv4TransportAddress_parsesHostAndPort() throws Exception {
        List<ProcessProfile> profiles = details.getNodeNetworkAndLogInfo(stats("10.0.0.1:5601"));

        assertEquals(1, profiles.size());
        assertEquals("10.0.0.1", profiles.get(0).httpPublishAddr);
        assertEquals(5601, profiles.get(0).httpPort);
    }

    @Test
    void ipv6TransportAddress_parsesHostAndPort() throws Exception {
        // Kibana configured with server.host: "::"
        List<ProcessProfile> profiles = details.getNodeNetworkAndLogInfo(stats(":::5601"));

        assertEquals(1, profiles.size());
        assertEquals("::", profiles.get(0).httpPublishAddr);
        assertEquals(5601, profiles.get(0).httpPort);
    }
}
