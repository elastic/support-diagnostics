package com.elastic.support.test;

import com.elastic.support.diagnostics.DiagnosticContext;
import com.elastic.support.diagnostics.commands.GetConfigCmd;
import org.junit.Test;


public class ReadConfigTest {

    @Test
    public void testReadConfig(){
        GetConfigCmd cmd = new GetConfigCmd();
        DiagnosticContext ctx = new DiagnosticContext();
        boolean rc = cmd.execute(ctx);
        assert rc;
        assert ctx.getConfig() != null;
    }
}
