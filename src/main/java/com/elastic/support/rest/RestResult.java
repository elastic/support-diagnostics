package com.elastic.support.rest;

import com.elastic.support.util.SystemProperties;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class RestResult implements Cloneable {

    private static final Logger logger = LogManager.getLogger(RestResult.class);

    String responseString;
    int status;


    public RestResult(HttpResponse response) {
        this.status = response.getStatusLine().getStatusCode();
        responseString = response.getEntity().toString();
        HttpClientUtils.closeQuietly(response);
    }

    public RestResult(HttpResponse response, String fileName) {

        try {
            this.status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                HttpEntity entity = response.getEntity();
                entity.writeTo(new FileOutputStream(fileName));
            } else {
                this.responseString = EntityUtils.toString(response.getEntity());
            }
        } catch (
                Exception e) {
            logger.log(SystemProperties.DIAG, "Error Processing Response", e);
            throw new RuntimeException();
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public String toString() {
        return responseString;
    }


}
