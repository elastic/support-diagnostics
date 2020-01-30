package com.elastic.support;

import com.elastic.support.util.ArchiveUtils;
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

public abstract  class BaseService {

    private Logger logger = LogManager.getLogger(BaseService.class);
    protected String logPath;

    protected void closeLogs() {

        logger.info("Closing logger.");

        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        Configuration logConfig = lc.getConfiguration();
        Appender appndr = logConfig.getAppender("File");
        if(appndr != null && appndr.isStarted()){
            appndr.stop();
        }

        logConfig.getRootLogger().removeAppender("File");

    }

    protected void createFileAppender(String logDir, String logFile) {

        logPath = logDir + SystemProperties.fileSeparator + logFile;

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration logConfig = context.getConfiguration();
        Layout layout = PatternLayout.newBuilder()
                .withConfiguration(logConfig)
                .withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")
                .build();

        FileAppender.Builder builder = FileAppender.newBuilder();
        builder.setConfiguration(logConfig);
        builder.withFileName(logPath);
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
        AppenderRef.createAppenderRef("File", SystemProperties.DIAG, null);
        logConfig.getRootLogger().addAppender(appender, Level.DEBUG, null);
        context.updateLoggers();

    }

    public void createArchive(String tempDir) {

        logger.info("Archiving diagnostic results.");

        try {
            String archiveFilename = SystemProperties.getFileDateString();
            ArchiveUtils archiveUtils = new ArchiveUtils();
            archiveUtils.createArchive(tempDir, archiveFilename);

        } catch (Exception ioe) {
            logger.info("Couldn't create archive. {}", ioe);
        }
    }

}
