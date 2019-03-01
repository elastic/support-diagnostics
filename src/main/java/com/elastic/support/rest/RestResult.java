package com.elastic.support.rest;

import com.elastic.support.util.SystemProperties;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.OutputStream;

public class RestResult implements Cloneable {

    private static final Logger logger = LogManager.getLogger(RestResult.class);

    String responseString;
    int status;

    public RestResult(HttpResponse response) {
        try{
            this.status = response.getStatusLine().getStatusCode();
            responseString = response.getEntity().toString();
        }
        catch (Exception e){
            logger.log(SystemProperties.DIAG, "Error Processing Response", e);
            throw new RuntimeException();
        }
        finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public RestResult(HttpResponse response, OutputStream out) {
        try{
            this.status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                response.getEntity().writeTo(out);
            } else {
                this.responseString = EntityUtils.toString(response.getEntity());
            }
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Error Streaming Response To OutputStream", e);
            throw new RuntimeException();
        }
        finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public String toString() {
        return responseString;
    }


}
