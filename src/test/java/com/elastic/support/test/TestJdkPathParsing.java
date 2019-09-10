package com.elastic.support.test;

import com.elastic.support.config.Constants;
import com.elastic.support.diagnostics.commands.SystemCallsCmd;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TestJdkPathParsing {

    private static final Logger logger = LogManager.getLogger(TestJdkPathParsing.class);
    SystemCallsCmd calls = new SystemCallsCmd();

    @Test
    public void checkWin() throws Exception {

        InputStream in = this.getClass().getClassLoader().getResourceAsStream("win-process-list.txt");
        String input = IOUtils.toString(in, "UTF-16");
        String jdk = calls.parseEsProcessString(input, "\\", "winOS");
        assertEquals("c:\\servers\\elasticsearch-7.3.1\\jdk", jdk);
    }

    @Test
    public void checkLinux() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("linux-process-list.txt");
        String input = IOUtils.toString(in, Constants.UTF8);
        String jdk = calls.parseEsProcessString(input, "/", "linuxOS");
        assertEquals("/usr/share/elasticsearch/jdk", jdk);
    }

    @Test
    public void checkMac() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("darwin-process-list.txt");
        String input = IOUtils.toString(in, Constants.UTF8);
        String jdk = calls.parseEsProcessString(input, "/", "macOS");
        assertEquals("/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home", jdk);
    }
}
