/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.rest;

import co.elastic.support.util.SystemProperties;
import co.elastic.support.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class RestResult implements Cloneable {

    private static final Logger logger = LogManager.getLogger(RestResult.class);

    String responseString = "Undetermined error = check logs";
    int status = -1;
    String reason;
    boolean isRetryable;
    String url = "";

    // Sending in a response object to be processed implicitly
    // closes the response as a result. The body is either streamed directly
    // to disk or the body is stored as a string and the status retained as well.
    public RestResult(HttpResponse response, String url) {
        this.url = url;
        try {
            processCodes(response);
            responseString = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            logger.error("Error Processing Response", e);
            throw new RuntimeException();
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public RestResult(HttpResponse response, String fileName, String url) {

        this.url = url;

        // If the query got a success status stream the result immediately to the target
        // file.
        // If not, the result should be small and contain diagnostic info so stgre it in
        // the response string.
        File output = new File(fileName);
        if (output.exists()) {
            FileUtils.deleteQuietly(output);
        }

        try (OutputStream out = new FileOutputStream(fileName)) {
            processCodes(response);
            response.getEntity().writeTo(out);
        } catch (Exception e) {
            logger.error("Error Streaming Response To OutputStream", e);
            throw new RuntimeException();
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    private void processCodes(HttpResponse response) {
        status = response.getStatusLine().getStatusCode();
        if (status == 400) {
            reason = "Bad Request. Rejected";
            isRetryable = true;
        } else if (status == 401) {
            reason = "Authentication failure. Invalid login credentials.";
            isRetryable = false;
        } else if (status == 403) {
            reason = "Authorization failure or invalid license.";
            isRetryable = false;
        } else if (status == 404) {
            reason = "Endpoint does not exist.";
            isRetryable = true;
        } else {
            reason = response.getStatusLine().getReasonPhrase();
            isRetryable = true;
        }
    }

    public String formatStatusMessage(String msg) {

        if (StringUtils.isNotEmpty(msg)) {
            msg = msg + " ";
        }

        return String.format("%sStatus: %d  Reason: %s",
                msg,
                status,
                reason);
    }

    public int getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String toString() {
        return responseString;
    }

    public void toFile(String fileName) {
        File output = new File(fileName);
        if (output.exists()) {
            FileUtils.deleteQuietly(output);
        }

        try (FileOutputStream fs = new FileOutputStream(output)) {
            IOUtils.write(reason + SystemProperties.lineSeparator + responseString, fs, Constants.UTF_8);
        } catch (Exception e) {
            logger.error("Error writing Response To OutputStream", e);
        }
    }

    public boolean isRetryable() {
        return isRetryable;
    }

    public boolean isValid() {
        if (status == 200) {
            return true;
        }
        return false;
    }

}
