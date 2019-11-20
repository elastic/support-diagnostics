package com.elastic.support.rest;

import com.elastic.support.util.SystemProperties;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class RestClient implements Closeable {

    private final Logger logger = LogManager.getLogger(RestClient.class);

    private CloseableHttpClient client;
    private HttpHost httpHost;
    private HttpClientContext httpContext;
    PoolingHttpClientConnectionManager manager;

    public RestClient(CloseableHttpClient client, HttpClientContext context, HttpHost httpHost) {
        this.client = client;
        this.httpContext = context;
        this.httpHost = httpHost;
    }

    public RestResult execQuery(String url) {
        return new RestResult(execGet(url, httpHost, httpContext), url);
    }

    public RestResult execQuery(String url, String fileName) {
        return new RestResult(execGet(url, httpHost, httpContext), fileName, url);
    }

    public HttpHost getHost(String host, int port, String scheme) {
        return new HttpHost(host, port, scheme);
    }

    public HttpClientContext getContext() {
        return HttpClientContext.create();
    }

    public HttpResponse execGet(String query, HttpHost httpHost, HttpClientContext httpContext) {
        HttpGet httpGet = new HttpGet(query);
        return execRequest(httpHost, httpGet, httpContext);
    }

    private HttpResponse execRequest(HttpHost httpHost, HttpRequestBase httpRequest, HttpClientContext httpContext) {
        try {
            return client.execute(httpHost, httpRequest, httpContext);
        } catch (HttpHostConnectException e) {
            logger.log(SystemProperties.DIAG, "Host connection error.", e);
            throw new RuntimeException("Host connection");
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Unexpected Execution Error", e);
            throw new RuntimeException("Unexpected exception");
        }
    }

    public HttpResponse execPost(String uri, String payload) {
        return execPost(uri, payload, httpHost, httpContext);
    }

    public HttpResponse execPost(String uri, String payload, HttpHost httpHost, HttpClientContext httpContext) {
        try {
            HttpPost httpPost = new HttpPost(uri);
            httpContext.getConnection();
            StringEntity entity = new StringEntity(payload);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            return execRequest(httpHost, httpPost, httpContext);
        } catch (UnsupportedEncodingException e) {
            logger.error("Error with json body.", e);
            throw new RuntimeException("Could not complete post request.");
        }
    }

    public HttpResponse execDelete(String uri) {
        HttpDelete httpDelete = new HttpDelete( uri);
        return execRequest(httpHost, httpDelete, httpContext);
    }

    public void close() {
        try {
            client.close();
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Error occurred closing client connection.");
        }
    }

}
