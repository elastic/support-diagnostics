package com.elastic.support.rest;

import com.elastic.support.util.SystemProperties;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.OutputStream;

public class RestClient implements Closeable {

    private final Logger logger = LogManager.getLogger(RestClient.class);

    private CloseableHttpClient client;
    private HttpHost httpHost;
    private HttpClientContext httpContext;;


    public RestClient(CloseableHttpClient client, HttpClientContext context, HttpHost httpHost){
        this.client = client;
        this.httpContext = context;
        this.httpHost = httpHost;
    }

    public RestResult execQuery(String url) {
        return new RestResult(exec(url, httpHost, httpContext));
    }

    public RestResult execQuery(String url, OutputStream out) {
        return new RestResult(exec(url, httpHost, httpContext), out);
    }

    public HttpResponse exec(String query, HttpHost httpHost, HttpClientContext httpContext) {
        try {
            HttpGet httpGet = new HttpGet(query);
            return client.execute(httpHost, httpGet, httpContext);
        } catch (HttpHostConnectException e) {
            logger.log(SystemProperties.DIAG, "Host connection error.", e);
            throw new RuntimeException("Host connection");
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Unexpected Execution Error", e);
            throw new RuntimeException("Unexpected exception");
        }
    }

    public HttpResponse exec(String query, String host, int port, String scheme){
        HttpClientContext httpContext = HttpClientContext.create();
        HttpHost httpHost = new HttpHost(host, port, scheme);
        return exec(query, httpHost, httpContext);
    }

    public void close(){
        try{
            client.close();
        }
        catch (Exception e){
            logger.log(SystemProperties.DIAG, "Error occurred closing client connection.");
        }
    }

}
