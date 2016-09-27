package com.elastic.support.test;

import com.elastic.support.diagnostics.InputParams;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.diagnostics.commands.DirectorySetupCmd;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;


public class DirectorySetupTest {

    @Test
    public void testDirectorySetup() throws  Exception{
        InputParams inputs = new InputParams();
        String targetDir = SystemProperties.userHome + SystemProperties.fileSeparator + "es-diag-output";
        inputs.setOutputDir(targetDir);
        DirectorySetupCmd cmd = new DirectorySetupCmd();
        DiagnosticContext ctx = new DiagnosticContext();
        ctx.setInputParams(inputs);
        ctx.setClusterName("test");
        boolean rc = cmd.execute(ctx);
        assert rc;

        File dir = new File(targetDir);
        assert(dir.exists());
        FileUtils.deleteDirectory(dir);


    }
}
