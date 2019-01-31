package com.elastic.support.test;

import com.elastic.support.diagnostics.commands.RunClusterQueriesCmd;
import com.elastic.support.util.JsonYamlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestRestCalls {
    private final Logger logger = LoggerFactory.getLogger(TestRestCalls.class);
    private RunClusterQueriesCmd cmd = new RunClusterQueriesCmd();
    private Map calls;

    @BeforeEach
    public void setupContext(){
        Map diags = JsonYamlUtils.readYamlFromClasspath("diags.yml", true);
        calls = (Map)diags.get("rest-calls");
    }

    @Test
    public void testVersionOne() {
        Map<String, String > statements = cmd.buildStatementsByVersion("1.6", calls);
        assertEquals( 37, statements.size());
    }

    @Test
    public void testVersionTwo(){
        Map<String, String > statements = cmd.buildStatementsByVersion("2.4", calls);
        assertEquals(44, statements.size());
    }

    @Test
    public void testVersionFive(){
        Map<String, String > statements = cmd.buildStatementsByVersion("5.6", calls);
        assertEquals(52, statements.size());
    }

    @Test
    public void testVersionSix(){
        Map<String, String > statements = cmd.buildStatementsByVersion("6.0", calls);
        assertEquals(54, statements.size());
    }

    @Test
    public void testVersionSixFour(){
        Map<String, String > statements = cmd.buildStatementsByVersion("6.4", calls);
        assertEquals(57, statements.size());
    }

    @Test
    public void testVersionSixFive(){
        Map<String, String > statements = cmd.buildStatementsByVersion("6.5", calls);
        assertEquals(59, statements.size());
    }

    @Test
    public void testVersionSeven(){
        Map<String, String > statements = cmd.buildStatementsByVersion("7.0", calls);
        assertEquals(59, statements.size());
    }

}
