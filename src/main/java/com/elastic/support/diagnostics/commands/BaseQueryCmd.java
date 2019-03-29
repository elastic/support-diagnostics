package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.DiagConfig;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.rest.RestCallManifest;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.util.*;

public abstract class BaseQueryCmd implements Command {

    /**
     * This class has shared functionality for both the Elasticsearch and
     * Logstash based REST calls. It interates through set of endpoints from the
     * configuration and executes each. In all cases the results are written
     * directly to disk to a successful access. For some specialized configured
     * cases such as the node and shard calls, a failure will result in a reattempt
     * after the configured number of seconds.
     */

    private final Logger logger = LogManager.getLogger(BaseQueryCmd.class);


    public RestCallManifest runQueries(RestClient restClient, Map<String, String> entries, String tempDir, DiagConfig diagConfig) {

        List<String> textExtensions = diagConfig.getTextFileExtensions();
        int attempts = diagConfig.getCallRetries();
        int pause = diagConfig.getPauseRetries();
        List<String> requireRetry = diagConfig.getRequireRetry();
        RestCallManifest restCallManifest = new RestCallManifest();
        Map<String, String> workEntries = new LinkedHashMap<>();
        workEntries.putAll(entries);

        // We will go through a max of three tries attmmpting to get file output
        // or until there are no more entries to get.
        // If an attempt is succesful it's written to disk and removed from the work entries map
        // If an attempt is unsuccessful and it's not in the retry list, remove it anyway.
        // If an attempt is unsuccessful and it's in the retry list, it will be there for the
        // next pass until you hit the limit or it succeeds. If we get to the last try and it
        // still hasn't worked, write the error content in the result to the target file.
        for (int i = 1; i <= attempts; i++) {
            // If everything worked last pass, get out
            if (workEntries.size() == 0) {
                break;
            }

            // We've made the first pass through - wait a few
            // seconds before trying again.
            if (i > 1 && i <= attempts) {
                try {
                    logger.info("Some calls failed: retrying in {} seconds.", pause / 1000);
                    Thread.sleep(pause);
                } catch (Exception e) {
                    logger.error("Failed pause on error.", e);
                }
            }

            restCallManifest.setRuns(i);
            Set<String> keys = workEntries.keySet();
            Set<String> removalEntries = new HashSet<>();

            for (String queryName : keys) {
                String query = workEntries.get(queryName);
                String filename = buildFileName(queryName, tempDir, textExtensions);
                logger.info("Running query:{} -  {}", queryName, query);
                RestResult restResult = runQuery(filename, query, restClient);
                // If it succeeded take it out of future work
                if (restResult.getStatus() == 200) {
                    removalEntries.add(queryName);
                    restCallManifest.setCallHistory(queryName, i, true);
                    logger.info("Results written to: {}", filename);
                } else {
                    // If it didn't succeed but it's not in the retry list or if it's not an
                    // error that's retryable such as an auth error remove it.
                    restCallManifest.setCallHistory(queryName, i, false);

                    if (!requireRetry.contains(queryName) || !isRetryable(restResult.getStatus())) {
                        removalEntries.add(queryName);
                        restResult.toFile(filename);
                        logger.info("Call failed. Bypassing.");

                    } else {
                        // If it failed, it's in the list and it's last try, write it out
                        if (i == attempts) {
                            restResult.toFile(filename);
                        }
                        else {
                            logger.info("Call failed: flagged for retry.");
                        }
                    }
                }
            }

            // if it was on the failed list remove it - do it this way to avoid modifying the
            // Collection you're iterating through
            for(String entry: removalEntries){
                workEntries.remove(entry);
            }
        }

        // Send back information on how many things didn't succeed and how many tries it took.
        // Not used except for unit tests currently
        return restCallManifest;
    }

    public RestResult runQuery(String filename, String url, RestClient restClient) {

        // At the end of this something should have been written to disk...
        try (FileOutputStream fs = new FileOutputStream(filename)) {
            return restClient.execQuery(url, fs);
        } catch (Exception e) {
            // Something happens just log it and go to the next query.
            logger.log(SystemProperties.DIAG, "Error occurred executing query {}", url, e);
            return new RestResult(url + ";" + e.getMessage());
        }
    }

    public String buildFileName(String queryName, String temp, List<String> extensions) {

        String ext;
        if (extensions.contains(queryName)) {
            ext = ".txt";
        } else {
            ext = ".json";
        }
        String fileName = temp + SystemProperties.fileSeparator + queryName + ext;

        return fileName;
    }

    public boolean isRetryable(int statusCode) {

        if (statusCode == 400) {
            logger.info("No data retrieved.");
            return true;
        } else if (statusCode == 401) {
            logger.info("Authentication failure: invalid login credentials. Check logs for details.");
            return false;
        } else if (statusCode == 403) {
            logger.info("Authorization failure or invalid license. Check logs for details.");
            return false;
        } else if (statusCode == 404) {
            logger.info("Endpoint does not exist.");
            return true;
        } else if (statusCode >= 500 && statusCode < 600) {
            logger.info("Unrecoverable server error.");
            return true;
        }

        return false;

    }


}
