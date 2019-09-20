package com.elastic.support.rest;

import com.elastic.support.config.Version;
import com.elastic.support.diagnostics.commands.RunClusterQueriesCmd;
import com.elastic.support.util.JsonYamlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestConfigureElasticRestCalls {
    private final Logger logger = LoggerFactory.getLogger(TestConfigureElasticRestCalls.class);
    private RunClusterQueriesCmd cmd = new RunClusterQueriesCmd();
    private Map calls;

    @BeforeEach
    public void setupContext(){
        Map diags = JsonYamlUtils.readYamlFromClasspath("diags-test.yml", true);
        calls = (Map)diags.get("rest-calls");
    }

    @Test
    public void testBaseVersione() {
        Map<String, String > statements = cmd.buildStatementsByVersion(new Version("1.0"), calls);
        assertEquals( 3, statements.size());
    }

    @Test
    public void testMinorRelease(){
        Map<String, String > statements = cmd.buildStatementsByVersion(new Version("2.4"), calls);
        assertEquals(4, statements.size());
    }

    @Test
    public void testMinorReleaseWithoutStatements(){
        Map<String, String > statements = cmd.buildStatementsByVersion(new Version("5.7"), calls);
        assertEquals(5, statements.size());
    }

    @Test
    public void testOverrides(){
        Map<String, String > statements = cmd.buildStatementsByVersion(new Version("5.6"), calls);
        assertEquals(5, statements.size());
    }


    @Test
    public void testUnreleasedVersion(){
        Map<String, String > statements = cmd.buildStatementsByVersion(new Version("7.0"), calls);
        assertEquals(7, statements.size());
    }

    @Test
    public void findJavaHHome(){
        String javaHome = System.getenv("JAVA_HOME");
        String props = System.getProperty("java.home");
        logger.info("");
    }

}
