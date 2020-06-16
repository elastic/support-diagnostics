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
    protected LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
    protected Configuration logConfig = loggerContext.getConfiguration();

    protected void closeLogs() {

        logger.info(Constants.CONSOLE, "Closing loggers.");

        Appender appndr = logConfig.getAppender("diag");
        if(appndr != null && appndr.isStarted()){
            appndr.stop();
        }

        logConfig.getRootLogger().removeAppender("File");

    }

    protected void createFileAppender(String logDir, String logFile) {

        logPath = logDir + SystemProperties.fileSeparator + logFile;

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
        if(oldAppender != null && oldAppender.isStarted()){
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

    public void createArchive(String tempDir) {

        logger.info(Constants.CONSOLE, "Archiving diagnostic results.");

        try {
            String archiveFilename = SystemProperties.getFileDateString();
            ArchiveUtils archiveUtils = new ArchiveUtils();
            archiveUtils.createArchive(tempDir, archiveFilename);

        } catch (Exception ioe) {
            logger.info("Couldn't create archive. {}", ioe);
        }
    }

}
