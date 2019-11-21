package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestEntry;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public abstract class BaseQueryCmd implements Command {

    private final Logger logger = LogManager.getLogger(BaseQueryCmd.class);

    /*
     * This class has shared functionality for both the Elasticsearch and
     * Logstash based REST calls. It interates through set of endpoints from the
     * configuration and executes each. In all cases the results are written
     * directly to disk to a successful access. For some specialized configured
     * cases such as the node and shard calls, a failure will result in a reattempt
     * after the configured number of seconds.
     */
    public int runQueries(RestClient restClient, List<RestEntry> entries, String tempDir, int retries, int pause) {

        // Run through the query list, first pass. If anything that's retryable failed the
        // RestEntry will be in the returned retry list.
        List<RestEntry> retryList = execQueryList(restClient, entries, tempDir);
        int totalRetries = retryList.size();

        for (int i = 0; i < retries; i++) {
            // If no failed entries are in the list, get out
            if (retryList.size() == 0) {
                break;
            }

            // Wait the configured pause time before trying again
            try {
                logger.info("Some calls failed but were flagged as recoverable: retrying in {} seconds.", pause / 1000);
                Thread.sleep(pause);
            } catch (Exception e) {
                logger.error("Failed pause on error.", e);
            }

            retryList = execQueryList(restClient, retryList, tempDir);
            totalRetries += retryList.size();

        }
        return totalRetries;
    }

    List<RestEntry> execQueryList(RestClient restClient, List<RestEntry> calls, String tempdir){

        List<RestEntry> retryFailed = new ArrayList<>();

        for (RestEntry entry : calls) {
            try {
                String subdir = entry.getSubdir();
                if(StringUtils.isEmpty(subdir)){
                    subdir = tempdir;
                }
                else {
                    subdir = tempdir + SystemProperties.fileSeparator + subdir;
                    File nestedFolder = new File(subdir);
                    if( ! nestedFolder.isDirectory() ){
                        nestedFolder.mkdir();
                    }
                }
                String fileName = subdir + SystemProperties.fileSeparator + entry.getName() + entry.getExtension();
                RestResult restResult = restClient.execQuery(entry.getUrl(), fileName);
                if (restResult.isValid()) {
                    logger.info("Results written to: {}", fileName);
                }
                else{
                    if(entry.isRetry() && restResult.isRetryable()){
                        retryFailed.add(entry);
                        logger.info("{}   {}  failed.", entry.getName(), entry.getUrl());
                        logger.info(restResult.formatStatusMessage("Flagged for retry."));
                    }
                    else{
                        logger.info("{}   {}  failed. Bypassing", entry.getName(), entry.getUrl());
                        logger.info(restResult.formatStatusMessage("See archived diagnostics.log for more detail."));
                    }
                }
            } catch (Exception e) {
                // Something happens just log it and go to the next query.
                logger.log(SystemProperties.DIAG, "Error occurred executing query {}", entry.getName() + " - " + entry.getUrl(), e);
                continue;
            }

        }
        return retryFailed;

    }

}
