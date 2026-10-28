/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.scrub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrubProcessorTest {

    private static final String NODES_INFO = "{\"cluster_name\":\"my-secret-cluster\","
            + "\"nodes\":{\"AbCdEfGhIjKlMnOpQrStUv\":{\"name\":\"secret-node-one\"},"
            + "\"ZyXwVuTsRqPoNmLkJiHgFe\":{\"name\":\"secret-node-two\"}}}";

    @Test
    void autoScrub_replacesClusterNodeIdsAndNodeNames() throws Exception {
        ScrubProcessor processor = new ScrubProcessor(NODES_INFO);

        String input = "cluster my-secret-cluster has nodes secret-node-one, secret-node-two "
                + "AbCdEfGhIjKlMnOpQrStUv ZyXwVuTsRqPoNmLkJiHgFe";
        String output = processor.processAutoscrub(input);

        assertFalse(output.contains("my-secret-cluster"), output);
        assertFalse(output.contains("secret-node-one"), output);
        assertFalse(output.contains("secret-node-two"), output);
        assertFalse(output.contains("AbCdEfGhIjKlMnOpQrStUv"), output);
        assertFalse(output.contains("ZyXwVuTsRqPoNmLkJiHgFe"), output);
        assertTrue(output.contains("has nodes"), "unrelated text must be preserved");
    }
}
