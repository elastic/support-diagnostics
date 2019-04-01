package com.elastic.support.diagnostics.local;

import com.elastic.support.BaseService;
import com.elastic.support.config.DiagConfig;
import com.elastic.support.config.LocalCollectionInputs;
import com.elastic.support.diagnostics.commands.CollectLogsCmd;
import com.elastic.support.diagnostics.commands.SystemCallsCmd;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class LocalCollectionService extends BaseService {

    private static final Logger logger = LogManager.getLogger(LocalCollectionService.class);

    public void exec(LocalCollectionInputs inputs, DiagConfig diagConfig){


        CollectLogsCmd collectLogsCmd = new CollectLogsCmd();

        int maxLogs = diagConfig.getLogSettings().get("maxLogs");
        int maxGcLogs = diagConfig.getLogSettings().get("maxGcLogs");
        String tempDir = inputs.getOutputDir() + SystemProperties.fileSeparator + "logs-and-syscalls";


        try {
            FileUtils.deleteDirectory(new File(tempDir));
            Files.createDirectories(Paths.get(tempDir));
        } catch (IOException e) {
            logger.error("Error creating output dir: {}", tempDir, e);
        }

        logger.info("Created output directory: {}", tempDir);

        // Set up the log file manually since we're going to package it with the diagnostic.
        // It will go to wherever we have the temp dir set up.
        logger.info("Configuring log file.");
        createFileAppender(tempDir, "local-collection.log");

        logger.info("Processing log files.");
        collectLogsCmd.collectLogs(inputs.getLogDir(), tempDir, maxLogs, maxGcLogs, true);

        SystemCallsCmd systemCallsCmd = new SystemCallsCmd();

        String os = systemCallsCmd.checkOS();
        Map<String, String> osCmds = diagConfig.getCommandMap(os);

        ProcessBuilder pb = systemCallsCmd.getProcessBuilder();
        systemCallsCmd.preProcessOsCmds(osCmds, inputs.getPid(), SystemProperties.javaHome);
        systemCallsCmd.processCalls(tempDir, osCmds, pb);

        closeLogs();
        createArchive(tempDir);
        SystemUtils.nukeDirectory(tempDir);

    }

}
