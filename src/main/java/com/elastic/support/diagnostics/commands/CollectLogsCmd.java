package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.Constants;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CollectLogsCmd implements Command {

    private final Logger logger = LogManager.getLogger(CollectLogsCmd.class);

    public void execute(DiagnosticContext context) {

        String logs = context.getLogDir();

        if (logs.equalsIgnoreCase(Constants.NOT_FOUND)) {
            logger.info("Could not locate log directory bypassing log collection.");
            return;
        }

        boolean getAccess = context.getDiagnosticInputs().isAccessLogs();
        logger.info("Processing log files.");

        try {
            int maxLogs = context.getDiagsConfig().getLogSettings().get("maxLogs");
            int maxGcLogs = context.getDiagsConfig().getLogSettings().get("maxGcLogs");

            // Create a directory for this node
            String nodeDir = context.getTempDir() + SystemProperties.fileSeparator + "logs";
            Files.createDirectories(Paths.get(nodeDir));
            File logDest = new File(nodeDir);
            File logDir = new File(logs);
            if (logDir.exists()) {

                //Get the top level log, slow search, and slow index logs
                Collection<File> logfiles = FileUtils.listFiles(logDir, new WildcardFileFilter("*.log"), null);
                List<File> keepers = new ArrayList<>();
                for (File logfile : logfiles) {
                    String name = logfile.getName();
                    if (name.contains("deprecation")) {
                        continue;
                    }
                    if (name.contains("access")) {
                        if (!getAccess) {
                            continue;
                        }
                    }
                    keepers.add(logfile);
                }

                for (File keeper : keepers) {
                    FileUtils.copyFileToDirectory(keeper, logDest);
                }

                String patternString = "*.log.gz";
                processLogVersions(patternString, maxLogs, logDir, logDest, false);
                patternString = "[a-zA-Z0-9_-]*$*.log.\\d{4}-\\d{2}-\\d{2}";
                processLogVersions(patternString, maxLogs, logDir, logDest, true);
                patternString = "gc*.log.*";
                processLogVersions(patternString, maxGcLogs, logDir, logDest, false);


            } else {
                logger.error("Configured log directory is not readable or does not exist: " + logDir.getAbsolutePath());
            }

        } catch (Exception e) {
            logger.error("Error processing logs: Error encountered reading directory. Does the account you are running under have sufficient permissions to read the log directories?");
            logger.error("Log directory: " + logs);
            logger.log(SystemProperties.DIAG, "Error reading log dir", e);
        }

        logger.info("Finished processing logs.");

    }

    private void processLogVersions(String pattern, int maxToGet, File logDir, File logDest, boolean useRegex) throws Exception {

        Collection<File> logs = null;
        if (useRegex) {
            logs = FileUtils.listFiles(logDir, new RegexFileFilter(pattern), null);
        } else {
            logs = FileUtils.listFiles(logDir, new WildcardFileFilter((new String[]{pattern})), null);
        }

        File logFileList[] = logs.toArray(new File[0]);
        //File[] logFileList = FileUtils.listFiles(logDir.getName(), logFileFilter, null);
        Arrays.sort(logFileList, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
        int limit = maxToGet, count = 0;
        for (File logfile : logFileList) {
            if (count < limit) {
                FileUtils.copyFileToDirectory(logfile, logDest);
                count++;
            } else {
                break;
            }
        }
    }

}
