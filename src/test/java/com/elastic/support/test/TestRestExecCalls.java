package com.elastic.support.test;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import com.elastic.support.util.JsonYamlUtils;
import org.junit.jupiter.api.BeforeAll;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mockserver.MockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestRestExecCalls {

    private final Logger logger = LoggerFactory.getLogger(TestRestExecCalls.class);
    private ClientAndServer mockServer;
    private Map config;

    @BeforeAll
    public void globalSetup9() {
        mockServer = startClientAndServer("localhost", 9200);
        config = JsonYamlUtils.readYamlFromClasspath("diags-test.yml", true);
    }

    @Test
    private void testSimpleQuery(){

    }
}
