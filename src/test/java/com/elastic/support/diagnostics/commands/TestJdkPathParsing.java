package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.JavaPlatform;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class TestJdkPathParsing {

/*
    private static final Logger logger = LogManager.getLogger(TestJdkPathParsing.class);
    CollectLocalSystemCalls calls = new CollectLocalSystemCalls();

    @Test
    public void checkWin() throws Exception {

        InputStream in = this.getClass().getClassLoader().getResourceAsStream("win-process-list.txt");
        String input = IOUtils.toString(in, "UTF-16");
        JavaPlatform javaPlatform = calls.checkOS("windows");
        String jdk = calls.parseEsProcessString(input, javaPlatform);
        assertEquals("c:\\servers\\elasticsearch-7.3.1\\jdk", jdk);
    }

    @Test
    public void checkLinux() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("linux-process-list.txt");
        String input = IOUtils.toString(in, Constants.UTF8);
        JavaPlatform javaPlatform = calls.checkOS("linux");
        String jdk = calls.parseEsProcessString(input, javaPlatform);
        assertEquals("/usr/share/elasticsearch/jdk", jdk);
    }

    @Test
    public void checkMac() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("darwin-process-list.txt");
        String input = IOUtils.toString(in, Constants.UTF8);
        JavaPlatform javaPlatform = calls.checkOS("darwin");
        String jdk = calls.parseEsProcessString(input, javaPlatform);
        assertEquals("/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home", jdk);
    }

    @Test
    public void checkWinPathParse(){

        JavaPlatform javaPlatform = calls.checkOS("windows");
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

        JavaPlatform javaPlatform = calls.checkOS("darwin");
        String path = "17049 s000  S+    15:45.88 /Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home/bin/java -Xms1g -Xmx1g -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -Des.networkaddress.cache.ttl=60 -Des.networkaddress.cache.negative.ttl=10 -XX:+AlwaysPreTouch -Xss1m -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djna.nosys=true -XX:-OmitStackTraceInFastThrow -Dio.netty.noUnsafe=true -Dio.netty.noKeySetOptimization=true -Dio.netty.recycler.maxCapacityPerThread=0 -Dlog4j.shutdownHookEnabled=false -Dlog4j2.disable.jmx=true -Djava.io.tmpdir=/var/folders/w7/wldkmsfs6nvdzkd53nk1w7rc0000gp/T/elasticsearch-2713839990115777019 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=data -XX:ErrorFile=logs/hs_err_pid%p.log -Xlog:gc*,gc+age=trace,safepoint:file=logs/gc.log:utctime,pid,tags:filecount=32,filesize=64m -Djava.locale.providers=COMPAT -Dio.netty.allocator.type=unpooled -Des.path.home=/Users/gnieman/Servers/elasticsearch-7.0.1 -Des.path.conf=/Users/gnieman/Servers/elasticsearch-7.0.1/config -Des.distribution.flavor=default -Des.distribution.type=tar -Des.bundled_jdk=true -cp /Users/gnieman/Servers/elasticsearch-7.0.1/lib/* org.elasticsearch.bootstrap.Elasticsearch";
        String javaHome = calls.extractJavaHome(path, javaPlatform);
        javaHome = javaPlatform.extraProcess(javaHome);
        assertEquals("/Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home", javaHome);

        path = "17049 s000  S+    15:45.88 /Library/Java/JavaVirtualMachines/jdk 12.jdk/Contents/Home/bin/java -Xms1g -Xmx1g -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -Des.networkaddress.cache.ttl=60 -Des.networkaddress.cache.negative.ttl=10 -XX:+AlwaysPreTouch -Xss1m -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djna.nosys=true -XX:-OmitStackTraceInFastThrow -Dio.netty.noUnsafe=true -Dio.netty.noKeySetOptimization=true -Dio.netty.recycler.maxCapacityPerThread=0 -Dlog4j.shutdownHookEnabled=false -Dlog4j2.disable.jmx=true -Djava.io.tmpdir=/var/folders/w7/wldkmsfs6nvdzkd53nk1w7rc0000gp/T/elasticsearch-2713839990115777019 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=data -XX:ErrorFile=logs/hs_err_pid%p.log -Xlog:gc*,gc+age=trace,safepoint:file=logs/gc.log:utctime,pid,tags:filecount=32,filesize=64m -Djava.locale.providers=COMPAT -Dio.netty.allocator.type=unpooled -Des.path.home=/Users/gnieman/Servers/elasticsearch-7.0.1 -Des.path.conf=/Users/gnieman/Servers/elasticsearch-7.0.1/config -Des.distribution.flavor=default -Des.distribution.type=tar -Des.bundled_jdk=true -cp /Users/gnieman/Servers/elasticsearch-7.0.1/lib/* org.elasticsearch.bootstrap.Elasticsearch";
        javaHome = calls.extractJavaHome(path, javaPlatform);
        javaHome = javaPlatform.extraProcess(javaHome);
        assertEquals("/Library/Java/JavaVirtualMachines/jdk 12.jdk/Contents/Home", javaHome);

    }

    @Test
    public void checkLinuxPathParse(){

        JavaPlatform javaPlatform = calls.checkOS("linux");
        String path = "elastic+  90046      1 19 16:43 ?        00:00:15 /usr/share/elasticsearch/jdk/bin/java -Xms1g -Xmx1g  -Des.path.home=/usr/share/elasticsearch ";
        String javaHome = calls.extractJavaHome(path, javaPlatform);
        javaHome = javaPlatform.extraProcess(javaHome);
        assertEquals("/usr/share/elasticsearch/jdk", javaHome);

        path = "elastic+  90046      1 19 16:43 ?        00:00:15 /usr/share/elastic search/jdk/bin/java -Xms1g -Xmx1g  -Des.path.home=/usr/share/elasticsearch ";
        javaHome = calls.extractJavaHome(path, javaPlatform);
        javaHome = javaPlatform.extraProcess(javaHome);
        assertEquals("/usr/share/elastic search/jdk", javaHome);

    }
*/

}
