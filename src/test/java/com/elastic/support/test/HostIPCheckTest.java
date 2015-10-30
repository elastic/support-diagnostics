package com.elastic.support.test;

import com.elastic.support.diagnostics.DiagnosticContext;
import com.elastic.support.diagnostics.commands.HostIdentifierCmd;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;


public class HostIPCheckTest {

    private final Logger logger = LoggerFactory.getLogger(HostIPCheckTest.class);

    @Test
    public void testHostIpCheck(){

        HostIdentifierCmd cmd = new HostIdentifierCmd();
        DiagnosticContext ctx = new DiagnosticContext();
        cmd.execute(ctx);
        Set hosts = ctx.getHostIpList();
        assert hosts.size() > 0;
        assert hosts.contains("localhost") == true;

    }

}
