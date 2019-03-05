package com.elastic.support.rest;

import com.elastic.support.config.Constants;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class RestResult implements Cloneable {

    private static final Logger logger = LogManager.getLogger(RestResult.class);

    String responseString;
    int status;
    String reason;

    // Sending in a response object to be processed implicitly
    // closes the response as a result. The body is either streamed directly
    // to disk or the body is stored as a string and the status retained as well.
    public RestResult(HttpResponse response) {
        try{
            this.status = response.getStatusLine().getStatusCode();
            this.reason = response.getStatusLine().getReasonPhrase();
            responseString = EntityUtils.toString(response.getEntity());
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

        // If the query got a success status stream the result immediately to the target file.
        // If not, the result should be small and contain diagnostic info so stgre it in the response string.
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

    public int getStatus(){
        return status;
    }

    public String getReason(){
        return reason;
    }

    public String toString() {
        return responseString;
    }

    public void toFile(String fileName){
        try(FileOutputStream fs = new FileOutputStream(fileName)){
            IOUtils.write(responseString, fs, Constants.UTF8);
        }
        catch (Exception e){
            logger.log(SystemProperties.DIAG, "Error writing Response To OutputStream", e);
        }
    }
}
