/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support;

import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.util.SystemProperties;
import co.elastic.support.util.ArchiveUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.File;

public abstract class BaseService {

    private Logger logger = LogManager.getLogger(BaseService.class);
    protected LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
    protected Configuration logConfig = loggerContext.getConfiguration();

    protected void closeLogs() {
        logger.info(Constants.CONSOLE, "Closing loggers.");

        Appender appender = logConfig.getAppender("packaged");
        if (appender != null && appender.isStarted()) {
            appender.stop();
        }

        logConfig.getRootLogger().removeAppender("packaged");
    }

    protected void createFileAppender(String logDir, String logFile) {
        Appender diagAppender = FileAppender.newBuilder()
                .setConfiguration(logConfig)
                .withFileName(logDir + SystemProperties.fileSeparator + logFile)
                .withAppend(false)
                .withLocking(false)
                .setName("packaged")
                .setIgnoreExceptions(false)
                .setLayout(
                        PatternLayout.newBuilder()
                                .withConfiguration(logConfig)
                                .withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")
                                .build())
                .build();

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

    public File createArchive(String tempDir) throws DiagnosticException {
        logger.info(Constants.CONSOLE, "Archiving diagnostic results.");
        return ArchiveUtils.createZipArchive(tempDir, SystemProperties.getFileDateString());
    }

}
