package com.elastic.support.rest;

import com.elastic.support.Constants;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;

public class RestResult extends AbstractHttpClientResponseHandler<RestResult> {

    private static final Logger logger = LogManager.getLogger(RestResult.class);

    String responseString = "Undetermined error = check logs";
    int status = -1;
    String reason;
    boolean isRetryable;
    File targetFile;

    public RestResult(){
    }

    public RestResult(String fileName) {
        targetFile = new File(fileName);
        if (targetFile.exists()) {
            FileUtils.deleteQuietly(targetFile);
        }
    }

    private String checkStatus(int status) {
        if (status == 400) {
            isRetryable = true;
            return "400 Bad Request. Rejected";
        } else if (status == 401) {
            isRetryable = false;
            return "401 Authentication failure. Invalid login credentials.";
        } else if (status == 403) {
            isRetryable = false;
            return "403 Authorization failure or invalid license.";
        } else if (status == 404) {
            isRetryable = false;
            return "404 Endpoint does not exist.";
        } else if (status == 405) {
            isRetryable = false;
            return "405 Method is not allowed.";
        } else {
            isRetryable = true;
            return status + "Other error.";
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

    public boolean isRetryable() {
        return isRetryable;
    }

    public boolean isValid() {
        return (status == 200);
    }

    protected void toFile(String content)  throws IOException {
        if(targetFile == null){
            return;
        }
        try (OutputStream out = new FileOutputStream(targetFile)) {
            IOUtils.write(content, out, Constants.UTF_8);
        }
        catch(IOException ioe){
            logger.error("File {} could not be written.", targetFile.getAbsolutePath(), ioe);
            throw ioe;
        }
    }

    protected void toFile(HttpEntity entity) throws IOException {
        responseString = "Results written to:" + targetFile.getAbsolutePath();
        try (OutputStream out = new FileOutputStream(targetFile)) {
            entity.writeTo(out);
        }
        catch(IOException ioe){
            logger.error("File {} could not be written.", targetFile.getAbsolutePath(), ioe);
            throw ioe;
        }
    }

    @Override
    public RestResult handleResponse(final ClassicHttpResponse response) throws IOException {

        HttpEntity entity = null;
        try {
            status = response.getCode();
            reason = status + SystemProperties.lineSeparator + response.getReasonPhrase();
            entity = response.getEntity();
            if (this.status > 200) {
                toFile("REST operation failed: " + " - " + checkStatus(status) + " - " + this.reason);
                logger.error("REST operation failed: {}, {}. ", checkStatus(status), this.reason);
                return this;
            }

            if (entity != null) {
                return handleEntity(entity);
            }
        } catch (Exception e) {
            this.status = -1;
            this.reason = e.getMessage();
            logger.error(e);
            throw new IOException(e);
        } finally {
            EntityUtils.consume(entity);
        }

        return this;
    }

    public RestResult handleEntity(HttpEntity entity) throws IOException {
        try {
            if (targetFile != null) {
                toFile(entity);
                responseString = "Output: " + targetFile.getAbsolutePath();
            } else {
                responseString =  EntityUtils.toString(entity);
            }
            return this;
        }
        catch (Exception e){
            throw new ClientProtocolException(e);
        }
    }
}

