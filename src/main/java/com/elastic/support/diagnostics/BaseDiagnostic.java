package com.elastic.support.diagnostics;

import com.beust.jcommander.JCommander;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract  class BaseDiagnostic implements Closeable {

    protected Logger logger = LogManager.getLogger(BaseDiagnostic.class);

    protected void closeLogs() {

        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        final Configuration config = lc.getConfiguration();
        Appender appndr = config.getAppender("File");
        appndr.stop();
        config.getRootLogger().removeAppender("File");
        logger.info("Log close complete.");

    }

    protected void nukeTempDir(String dir){
        try {
            File tmp = new File(dir);
            tmp.setWritable(true, false);
            FileUtils.deleteDirectory(tmp);
            logger.info("Deleted temp directory: {}.", dir);
        } catch (IOException e) {
            logger.error("Access issue with temp directory", e);
        }
    }

    protected void createFileAppender(String logDir, String logFile) {

        logDir = logDir + SystemProperties.fileSeparator + logFile;

        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration config = context.getConfiguration();
      /*Layout layout = PatternLayout.createLayout("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n", null, config, null,
         null,true, true, null, null );*/
        Layout layout = PatternLayout.newBuilder()
                .withConfiguration(config)
                .withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")
                .build();

        FileAppender.Builder builder = FileAppender.newBuilder();
        builder.setConfiguration(config);
        builder.withFileName(logDir);
        builder.withAppend(false);
        builder.withLocking(false);
        builder.withName("File");
        builder.withIgnoreExceptions(false);
        builder.withImmediateFlush(true);
        builder.withBufferedIo(false);
        builder.withBufferSize(0);
        builder.withLayout(layout);
        builder.withAdvertise(false);
        Appender appender = builder.build();

        appender.start();
        config.addAppender(appender);
        AppenderRef.createAppenderRef("File", null, null);
        config.getRootLogger().addAppender(appender, Level.DEBUG, null);
        context.updateLoggers();

    }


    public abstract void exec();
    public abstract void close();
}
