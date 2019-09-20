package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.Constants;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class TestJdkPathParsing {

    private static final Logger logger = LogManager.getLogger(TestJdkPathParsing.class);
    SystemCallsCmd calls = new SystemCallsCmd();

    @Test
    public void checkWin() throws Exception {

        InputStream in = this.getClass().getClassLoader().getResourceAsStream("win-process-list.txt");
        String input = IOUtils.toString(in, "UTF-16");
        SystemCallsCmd.JavaPlatform javaPlatform = calls.checkOS("windows");
        String jdk = calls.parseEsProcessString(input, javaPlatform);
        assertEquals("c:\\servers\\elasticsearch-7.3.1\\jdk", jdk);
    }

    @Test
    public void checkLinux() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("linux-process-list.txt");
        String input = IOUtils.toString(in, Constants.UTF8);
        SystemCallsCmd.JavaPlatform javaPlatform = calls.checkOS("linux");
        String jdk = calls.parseEsProcessString(input, javaPlatform);
        assertEquals("/usr/share/elasticsearch/jdk", jdk);
    }

    @Test
    public void checkMac() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("darwin-process-list.txt");
        String input = IOUtils.toString(in, Constants.UTF8);
        SystemCallsCmd.JavaPlatform javaPlatform = calls.checkOS("darwin");
        String jdk = calls.parseEsProcessString(input, javaPlatform);
        assertEquals("/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home", jdk);
    }

    @Test
    public void checkWinPathParse(){

        SystemCallsCmd.JavaPlatform javaPlatform = calls.checkOS("windows");
        String path = "java.exe             \"c:\\servers\\elasticsearch-7.3.1\\jdk\\bin\\java.exe\"   -Des.networkaddress.cache.ttl=60";
        String javaHome = calls.extractJavaHome(path, javaPlatform);
        javaHome = javaPlatform.extraProcess(javaHome);
        assertEquals("c:\\servers\\elasticsearch-7.3.1\\jdk", javaHome);

        path = "java.exe             \"c:\\server applications\\elasticsearch 7.3.1\\jdk\\bin\\java.exe\"   -Des.networkaddress.cache.ttl=60";
        javaHome = calls.extractJavaHome(path, javaPlatform);
        javaHome = javaPlatform.extraProcess(javaHome);
        assertEquals("c:\\server applications\\elasticsearch 7.3.1\\jdk", javaHome);

    }

    @Test
    public void checkMacPathParse(){

        SystemCallsCmd.JavaPlatform javaPlatform = calls.checkOS("darwin");
        String path = "  502 37582 37576   0 11:20AM ttys000    0:32.65 /Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home/bin/java  -Des.networkaddress.cache.ttl=60 ";
        String javaHome = calls.extractJavaHome(path, javaPlatform);
        javaHome = javaPlatform.extraProcess(javaHome);
        assertEquals("/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home", javaHome);

        path = "  502 37582 37576   0 11:20AM ttys000    0:32.65 /Some Path/jdk-12.jdk/Contents/Home/bin/java  -Des.networkaddress.cache.ttl=60 ";
        javaHome = calls.extractJavaHome(path, javaPlatform);
        javaHome = javaPlatform.extraProcess(javaHome);
        assertEquals("/Some Path/jdk-12.jdk/Contents/Home", javaHome);

    }

    @Test
    public void checkLinuxPathParse(){

        SystemCallsCmd.JavaPlatform javaPlatform = calls.checkOS("linux");
        String path = "elastic+  90046      1 19 16:43 ?        00:00:15 /usr/share/elasticsearch/jdk/bin/java -Xms1g -Xmx1g  -Des.path.home=/usr/share/elasticsearch ";
        String javaHome = calls.extractJavaHome(path, javaPlatform);
        javaHome = javaPlatform.extraProcess(javaHome);
        assertEquals("/usr/share/elasticsearch/jdk", javaHome);

        path = "elastic+  90046      1 19 16:43 ?        00:00:15 /usr/share/elastic search/jdk/bin/java -Xms1g -Xmx1g  -Des.path.home=/usr/share/elasticsearch ";
        javaHome = calls.extractJavaHome(path, javaPlatform);
        javaHome = javaPlatform.extraProcess(javaHome);
        assertEquals("/usr/share/elastic search/jdk", javaHome);

    }

}
