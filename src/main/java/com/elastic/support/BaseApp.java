package com.elastic.support;

import com.elastic.support.rest.ElasticRestClientInputs;
import com.elastic.support.rest.RestClient;
import com.elastic.support.util.ArchiveUtils;
import com.elastic.support.util.ResourceUtils;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public abstract class BaseApp {

    private static final Logger logger = LogManager.getLogger(BaseApp.class);

    protected static void initInputs(String args[],
                                     BaseInputs inputs) {
        if (args.length == 0) {
            logger.info(Constants.CONSOLE, Constants.interactiveMsg);
            inputs.interactive = true;
            inputs.runInteractive();
        } else {
            List<String> errors = inputs.parseInputs(args);
            if (errors.size() > 0) {
                for (String err : errors) {
                    logger.error(Constants.CONSOLE, err);
                }
                inputs.usage();
                SystemUtils.quitApp();
            }
        }

        inputs.tempDir = ResourceUtils.createTempDirectory(inputs.outputDir);

    }

    protected static void elasticsearchConnection(ElasticRestClientInputs inputs, BaseConfig config){
        ResourceUtils.restClient = new RestClient(inputs, config);
    }

    protected static void githubConnection(BaseConfig config){
        ResourceUtils.githubRestClient = new RestClient(
                config.diagHost,
                true,
                true,
                "",
                "",
                "",
                "",
                "",
                config.connectionTimeout,
                config.connectionRequestTimeout,
                config.socketTimeout,
                config.maxTotalConn,
                config.maxConnPerRoute,
                config.idleExpire
        );
    }
}
