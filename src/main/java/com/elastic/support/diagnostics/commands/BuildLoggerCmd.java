package com.elastic.support.diagnostics.commands;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;


import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import com.elastic.support.SystemProperties;
import com.elastic.support.diagnostics.DiagnosticContext;
import org.slf4j.LoggerFactory;


public class BuildLoggerCmd extends AbstractDiagnosticCmd {

   @Override
   public boolean execute(DiagnosticContext context) {

      String dir = context.getTempDir();

      Logger progLog = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
      LoggerContext loggerContext = progLog.getLoggerContext();
      PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
      logEncoder.setContext(loggerContext);
      logEncoder.setPattern("%d{ISO8601} [%thread] %-5level %logger{5} - %msg%n");
      logEncoder.start();
      FileAppender logFileAppender = new FileAppender();
      logFileAppender.setName("localLogFile");
      logFileAppender.setEncoder(logEncoder);
      logFileAppender.setContext(loggerContext);
      logFileAppender.setFile(dir + SystemProperties.fileSeparator +"diagnostics.log");
      logFileAppender.start();
      progLog.addAppender(logFileAppender);

      return true;
   }
}




