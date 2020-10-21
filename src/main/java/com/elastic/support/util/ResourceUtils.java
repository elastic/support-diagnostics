package com.elastic.support.util;

import com.elastic.support.Constants;
import com.elastic.support.rest.RestClient;
import jline.console.ConsoleReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.beryx.textio.jline.JLineTextTerminal;

import java.io.File;
import java.util.concurrent.ExecutorService;


public class ResourceUtils {

    public static final TextIO textIO = TextIoFactory.getTextIO();
    public static final TextTerminal terminal = textIO.getTextTerminal();
    private static final Logger logger = LogManager.getLogger(ResourceUtils.class);
    public static RestClient restClient, githubRestClient;
    public static SystemCommand systemCommand;
    public static ExecutorService executorService = null;

    static {
        if (terminal instanceof JLineTextTerminal) {
            JLineTextTerminal jltt = (JLineTextTerminal) terminal;
            ConsoleReader reader = jltt.getReader();
            reader.setExpandEvents(false);
        }
    }

    // Centralized method for cleaning up console, ssh and http clients
    public static void closeAll() {
        try {
            restClient.close();
            githubRestClient.close();
        } catch (Exception e) {
            logger.error("Failed to close all Http Clients.");
        }
        try {
            systemCommand.close();
        } catch (Exception e) {
            logger.error("Failed to close System commands console.");
        }
        try {
            textIO.dispose();
        } catch (Exception e) {
            logger.error("Failed to close terminal input window.");
        }
        try{
            if(executorService != null){
                executorService.shutdown();
            }
        }
        catch (Exception e){
            logger.error("Could no shut down workers pool.");
        }

    }

    public static String createTempDirectory(String outputDir) {
        try {
            // Create the temp directory - delete if first if it exists from a previous run
            String tempDir = outputDir + SystemProperties.fileSeparator + Constants.ES_DIAG_TEMP;
            logger.info(Constants.CONSOLE, "{}Creating temp directory: {}", SystemProperties.lineSeparator, tempDir);
            SystemUtils.refreshDir(tempDir);
            return tempDir;
        } catch (Exception e) {
            logger.error("Could not create temporary directory. Check your directory structure or permissions.");
            throw new RuntimeException("Initialization failure - creating temp directory", e);
        }
    }

    public static void startLog(String logPath) {
        // Modify the log file setup since we're going to package it with the diagnostic.
        // The log4 configuration file sets up 2 loggers, one strictly for the console and a file log in the working directory to handle
        // any errors we get at the library level that occur before we can get it initiailized.  When we have a target directory to
        // redirect output to we can reconfigure the appender to go to the diagnostic output temp directory for packaging with the archive.
        // This lets you configure and create loggers via the file if you want to up the level on one of the library dependencies as well
        // as internal classes.
        // If you want the output to also be shown on the console use: logger.info/error/warn/debug(Constants.CONSOLE, "Some log message");
        // This will also log that same output to the diagnostic log file.
        // To just log to the file log as normal: logger.info/error/warn/debug("Log mewssage");
        logger.info(Constants.CONSOLE, "Configuring log file {}.", logPath);
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration logConfig = loggerContext.getConfiguration();

        Layout layout = PatternLayout.newBuilder()
                .withConfiguration(logConfig)
                .withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")
                .build();

        FileAppender.Builder builder = FileAppender.newBuilder();
        builder.setConfiguration(logConfig);
        builder.withFileName(logPath);
        builder.withAppend(false);
        builder.withLocking(false);
        builder.setName("packaged");
        builder.setIgnoreExceptions(false);
        builder.withImmediateFlush(true);
        builder.withBufferedIo(false);
        builder.withBufferSize(0);
        builder.setLayout(layout);
        Appender diagAppender = builder.build();

        Appender oldAppender = logConfig.getAppender("packaged");
        if (oldAppender != null && oldAppender.isStarted()) {
            oldAppender.stop();
            logConfig.getRootLogger().removeAppender("packaged");
        }

        diagAppender.start();
        logConfig.addAppender(diagAppender);
        AppenderRef.createAppenderRef("packaged", null, null);
        logConfig.getRootLogger().addAppender(diagAppender, null, null);
        loggerContext.updateLoggers();
        logger.info(Constants.CONSOLE, "Diagnostic logger reconfigured for inclusion into archive");

    }

    public static void closeFileLogs() {

        logger.info(Constants.CONSOLE, "Closing loggers.");
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration logConfig = loggerContext.getConfiguration();
        Appender appndr = logConfig.getAppender("diag");
        if(appndr != null && appndr.isStarted()){
            appndr.stop();
        }

        logConfig.getRootLogger().removeAppender("File");

    }
}
