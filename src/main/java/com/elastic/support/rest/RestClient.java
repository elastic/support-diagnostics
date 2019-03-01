package com.elastic.support.rest;

import com.elastic.support.util.SystemProperties;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.OutputStream;

public class RestClient implements Closeable {

    private final Logger logger = LogManager.getLogger(RestClient.class);

    public static int DEEFAULT_ES_PORT = 9200;
    public static int DEEFAULT_HTTP_PORT = 80;
    public static int DEEFAULT_HTTPS_PORT = 443;

    private CloseableHttpClient client;
    private HttpHost httpHost;
    private HttpClientContext httpContext;;
    private AuthScope authScope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
    private CredentialsProvider credentialsProvider;;
    AuthCache authCache = new BasicAuthCache();
    BasicScheme basicScheme = new BasicScheme();


    public RestClient(CloseableHttpClient client, CredentialsProvider credentialsProvider){
        this.client = client;
        this.credentialsProvider = credentialsProvider;
    }

    public void configureDestination(String host, int port, String scheme){
        this.httpContext = HttpClientContext.create();
        this.httpHost = new HttpHost(host, port, scheme);
    }

    public void configureDestination(String host, int port, String scheme, String user, String password){
        configureDestination(host, port, scheme);
        setCredentials(user, password);
        authCache.put(httpHost, basicScheme);
        this.httpContext.setAuthCache(authCache);
    }

    public void setCredentials(String user, String password){
        credentialsProvider.setCredentials(
                authScope,
                new UsernamePasswordCredentials(user, password));
        this.httpContext.setCredentialsProvider(credentialsProvider);
    }

    public RestResult execQuerye(String url) {
        return new RestResult(execGet(url));
    }

    public RestResult execQuery(String url, OutputStream out) {
        return new RestResult(execGet(url), out);
    }

    public HttpResponse execGet(String query) {
        try {
            HttpGet httpget = new HttpGet(query);
            return client.execute(httpHost, httpget, httpContext);
        } catch (HttpHostConnectException e) {
            logger.log(SystemProperties.DIAG, "Host connection error.", e);
            throw new RuntimeException("Host connection");
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Unexpected Execution Error", e);
            throw new RuntimeException("Unexpected exception");
        }
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
