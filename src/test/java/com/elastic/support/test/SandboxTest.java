package com.elastic.support.test;


import com.elastic.support.diagnostics.commands.AbstractDiagnosticCmd;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;

public class SandboxTest {

   protected static final Logger logger = LoggerFactory.getLogger(AbstractDiagnosticCmd.class);

    @Test
    public void testGenericCode() throws Exception{


       HashMap hm = new HashMap<>();
       hm.put("test", new String[] {"val1", "val2"} );
       Object ret = hm.get("test");
       logger.error("test");
    }
}
