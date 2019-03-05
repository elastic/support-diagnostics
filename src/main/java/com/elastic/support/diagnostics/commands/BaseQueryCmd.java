package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.DiagConfig;
import com.elastic.support.config.DiagnosticInputs;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestExec;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseQueryCmd implements Command {

    private final Logger logger = LogManager.getLogger(BaseQueryCmd.class);

    public void runQueries(RestClient restClient, Map<String, String> entries, String tempDir, DiagConfig diagConfig) {
        List<String> textExtensions = diagConfig.getTextFileExtensions();
        Map<String, Integer> retrySettings = diagConfig.getCallRetries();

        for (Map.Entry<String, String> entry : entries.entrySet()) {
            String queryName = entry.getKey();
            int attempts = retrySettings.get(queryName);
            String query = entry.getValue();
            String filename = buildFileName(queryName, tempDir, textExtensions);
            runQuery(filename, query, restClient, attempts);
        }

    }

    public RestResult runQuery(String filename, String url, RestClient restClient, int attempts) {

        // At the end of this something should have been written to disk...
        try (FileOutputStream fs = new FileOutputStream(filename)) {

            // Some queries such as the nodes and shards related will be reattempted for a non 200 response status based on configuration up to a configured limit.
            // If you get a successful run, just break out of the loop. Yes, I know some people don't like break. I do for cases like this.
            // When the number of configured attempts is exhausted with a non-200 status the response body containing the error will be written to the
            // file and it proceeds to the next query.
            for (int i = 0; i < attempts; i++) {
                RestResult restResult = restClient.execQuery(url, fs);
                if (restResult.getStatus() == 200) {
                    return restResult;
                } else {
                    logger.info("Unsuccessful query attempt for: {}. Attempting again in 5 seconds.", url);
                    logger.log(SystemProperties.DIAG, "Status: {}, Status Reason: {}", restResult.getStatus(), restResult.getReason());
                    if ((i + 1) == attempts) {
                        restResult.toFile(filename);
                        return restResult;
                    }
                    else{
                        if (isRetryable(restResult.getStatus())){
                            Thread.sleep(5000);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Something happens just log it and go to the nexe query.
            logger.log(SystemProperties.DIAG, "Error occurred executing query {}", url, e);
        }

        return null;
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
        } else if (statusCode > 500 && statusCode < 600) {
            logger.info("Unrecoverable server error.");
            return true;
        }

        return false;

    }


}