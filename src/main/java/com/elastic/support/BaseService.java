package com.elastic.support;

import com.elastic.support.rest.RestExec;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
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

import java.io.Closeable;
import java.util.Map;

public abstract  class BaseService implements Closeable {

    protected Logger logger = LogManager.getLogger(BaseService.class);
    protected static Map config;
    protected static RestExec restExec;
    protected static Map chains;


    public BaseService(){

        // Initialize the configuration files. This is common to any of the app runs.
        try {
            this.config = JsonYamlUtils.readYamlFromClasspath("diags.yml", true);
            if (this.config.size() == 0) {
                logger.error("Required config file diags.yml was not found. Exiting application.");
                throw new RuntimeException("Missing diags.yml");
            }

            this.chains = JsonYamlUtils.readYamlFromClasspath("chains.yml", false);
            if (chains.size() == 0) {
                logger.error("Required config file chains.yml was not found. Exiting application.");
                throw new RuntimeException("Missing chain.yml");
            }

        } catch (Exception e) {
            logger.error("Error encountered running diagnostic. See logs for additional information.  Exiting application.", e);
            throw new RuntimeException("DiagnosticService runtime error", e);
        }

    }

    protected void closeLogs() {

        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        final Configuration logConfig = lc.getConfiguration();
        Appender appndr = logConfig.getAppender("File");
        appndr.stop();
        logConfig.getRootLogger().removeAppender("File");
        logger.info("Log close complete.");

    }

    protected void createFileAppender(String logDir, String logFile) {

        logDir = logDir + SystemProperties.fileSeparator + logFile;

        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration logConfig = context.getConfiguration();
      /*Layout layout = PatternLayout.createLayout("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n", null, logConfig, null,
         null,true, true, null, null );*/
        Layout layout = PatternLayout.newBuilder()
                .withConfiguration(logConfig)
                .withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")
                .build();

        FileAppender.Builder builder = FileAppender.newBuilder();
        builder.setConfiguration(logConfig);
        builder.withFileName(logDir);
        builder.withAppend(false);
        builder.withLocking(false);
        builder.setName("File");
        builder.setIgnoreExceptions(false);
        builder.withImmediateFlush(true);
        builder.withBufferedIo(false);
        builder.withBufferSize(0);
        builder.setLayout(layout);
        builder.withAdvertise(false);
        Appender appender = builder.build();

        appender.start();
        logConfig.addAppender(appender);
        AppenderRef.createAppenderRef("File", null, null);
        logConfig.getRootLogger().addAppender(appender, Level.DEBUG, null);
        context.updateLoggers();

    }

    public abstract void exec();
    public abstract void close();
}
