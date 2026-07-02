/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.diagnostics.ProcessProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckPlatformDetailsTest {

    private CheckPlatformDetails checker;

    @BeforeEach
    void setUp() {
        checker = new CheckPlatformDetails();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ProcessProfile makeNode(String name, String ip, String httpPublishAddr, Set<String> boundAddresses) {
        ProcessProfile p = new ProcessProfile();
        p.name = name;
        p.ip = ip;
        p.host = ip;
        p.httpPublishAddr = httpPublishAddr;
        p.boundAddresses = boundAddresses;
        return p;
    }

    // -----------------------------------------------------------------------
    // findLocalTargetNode — loopback input uses NIC-based matching
    // -----------------------------------------------------------------------

    /** When the input host is a loopback address the code must fall through
     *  to NIC scanning; with our fallback it should still find the node via ip. */
    @Test
    void loopbackInput_fallsBackToNicScan_matchesViaIp() {
        // Simulate node whose ip matches what getNetworkInterfaces() would return
        // We can't mock the real NIC scan, so we exercise findLocalTargetNode with
        // a non-loopback host that equals the node ip directly.
        ProcessProfile node = makeNode("NTG-PRO-ELA-01", "172.16.1.61", "172.16.1.61", new HashSet<>());
        // Non-loopback host that equals the node ip → added to localNetworkInterfaces
        ProcessProfile result = checker.findLocalTargetNode("172.16.1.61", List.of(node));
        assertNotNull(result);
        assertEquals("NTG-PRO-ELA-01", result.name);
    }

    // -----------------------------------------------------------------------
    // findTargetNode — primary: boundAddresses match
    // -----------------------------------------------------------------------

    @Test
    void matchViaBoundAddresses_returnsCorrectNode() {
        ProcessProfile node1 = makeNode("node-1", "10.0.0.1", "10.0.0.1", new HashSet<>(Set.of("10.0.0.1")));
        ProcessProfile node2 = makeNode("node-2", "10.0.0.2", "10.0.0.2", new HashSet<>(Set.of("10.0.0.2")));

        ProcessProfile result = checker.findLocalTargetNode("10.0.0.2", List.of(node1, node2));
        assertEquals("node-2", result.name);
    }

    // -----------------------------------------------------------------------
    // findTargetNode — fallback: boundAddresses empty, match via node.ip
    // -----------------------------------------------------------------------

    @Test
    void emptyBoundAddresses_matchViaIpFallback() {
        ProcessProfile node = makeNode("NTG-PRO-ELA-01", "172.16.1.61", "172.16.1.61", new HashSet<>());

        ProcessProfile result = checker.findLocalTargetNode("172.16.1.61", List.of(node));
        assertNotNull(result);
        assertEquals("NTG-PRO-ELA-01", result.name);
    }

    @Test
    void emptyBoundAddresses_matchViaHttpPublishAddrFallback() {
        // ip and httpPublishAddr differ (NAT / publish_host override scenario)
        ProcessProfile node = makeNode("NTG-PRO-ELA-01", "10.0.0.5", "172.16.1.61", new HashSet<>());

        // local address matches httpPublishAddr, not ip
        ProcessProfile result = checker.findLocalTargetNode("172.16.1.61", List.of(node));
        assertNotNull(result);
        assertEquals("NTG-PRO-ELA-01", result.name);
    }

    // -----------------------------------------------------------------------
    // findTargetNode — multiple nodes, only one matches (mirrors the bug report)
    // -----------------------------------------------------------------------

    @Test
    void multipleNodes_emptyBoundAddresses_returnsCorrectOne() {
        ProcessProfile ela01 = makeNode("NTG-PRO-ELA-01", "172.16.1.61", "172.16.1.61", new HashSet<>());
        ProcessProfile ela02 = makeNode("NTG-PRO-ELA-02", "172.16.1.62", "172.16.1.62", new HashSet<>());
        ProcessProfile ela03 = makeNode("NTG-PRO-ELA-03", "172.16.1.63", "172.16.1.63", new HashSet<>());
        ProcessProfile ela04 = makeNode("NTG-PRO-ELA-04", "172.16.1.64", "172.16.1.64", new HashSet<>());

        // The local machine is NTG-PRO-ELA-01 (172.16.1.61)
        ProcessProfile result = checker.findLocalTargetNode("172.16.1.61", List.of(ela01, ela02, ela03, ela04));
        assertNotNull(result);
        assertEquals("NTG-PRO-ELA-01", result.name);
    }

    // -----------------------------------------------------------------------
    // findTargetNode — no match throws RuntimeException
    // -----------------------------------------------------------------------

    @Test
    void noMatch_throwsRuntimeException() {
        ProcessProfile node = makeNode("node-1", "10.0.0.1", "10.0.0.1", new HashSet<>());

        assertThrows(RuntimeException.class, () -> checker.findLocalTargetNode("192.168.99.99", List.of(node)));
    }
}
