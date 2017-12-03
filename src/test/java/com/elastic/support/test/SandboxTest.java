package com.elastic.support.test;


import com.elastic.support.diagnostics.commands.AbstractDiagnosticCmd;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;

public class SandboxTest {

   protected static final Logger logger = LoggerFactory.getLogger(AbstractDiagnosticCmd.class);

    @Test
    public void testGenericCode() throws Exception{


       String tst = "".replace("PID", "");
       logger.info("\n" +
          "        test");
    }





}
