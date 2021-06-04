/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package com.elastic.support.rest;

import com.elastic.support.Constants;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RestClient implements Closeable {

    private static final Logger logger = LogManager.getLogger(RestClient.class);
    private static final int maxTotal = 100, defaultMaxPerRoute = 10;

    private static ConcurrentMap<String, RestClient> cachedClients = new ConcurrentHashMap<>();
    private CloseableHttpClient client;
    private HttpHost httpHost;
    private HttpClientContext httpContext;

    public RestClient(CloseableHttpClient client, HttpHost httpHost, HttpClientContext context) {
        this.client = client;
        this.httpHost = httpHost;
        this.httpContext = context;
    }

    public RestResult execQuery(String url) {
        return new RestResult(execGet(url), url);
    }

    public RestResult execQuery(String url, String fileName) {
        return new RestResult(execGet(url), fileName, url);
    }

    public HttpResponse execGet(String query) {
        HttpGet httpGet = new HttpGet(query);
        logger.debug(query);
        return execRequest(httpGet);
    }

    private HttpResponse execRequest(HttpRequestBase httpRequest) {
        try {
            return client.execute(httpHost, httpRequest, httpContext);
        } catch (HttpHostConnectException e) {
            logger.error( "Host connection error.", e);
            throw new RuntimeException("Host connection");
        } catch (Exception e) {
            logger.error( "Unexpected Execution Error", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public HttpResponse execPost(String uri, String payload) {
        try {
            HttpPost httpPost = new HttpPost(uri);
            StringEntity entity = new StringEntity(payload);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            logger.debug(uri + SystemProperties.fileSeparator + payload);
            return execRequest(httpPost);
        } catch (UnsupportedEncodingException e) {
            logger.error(Constants.CONSOLE,  "Error with json body.", e);
            throw new RuntimeException("Could not complete post request.");
        }
    }

    public HttpResponse execDelete(String uri) {
        HttpDelete httpDelete = new HttpDelete( uri);
        logger.debug(uri);

        return execRequest(httpDelete);
    }

    public void close() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            logger.error( "Error occurred closing client connection.");
        }
    }

    public static RestClient getClient(
            String host,
            int port,
            String scheme,
            String user,
            String password,
            String proxyHost,
            int proxyPort,
            String proxyUser,
            String proxyPassword,
            String pkiKeystore,
            String pkiKeystorePass,
            boolean bypassVerify,
            int connectionTimeout,
            int connectionRequestTimeout,
            int socketTimeout){

        try{
            HttpClientBuilder clientBuilder = HttpClients.custom();
            HttpHost httpHost = new HttpHost(host, port, scheme);
            HttpHost httpProxyHost = null;
            HttpClientContext context = HttpClientContext.create();

            // Create AuthCache instance
            AuthCache authCache = new BasicAuthCache();
            // Generate BASIC scheme object and add it to the local auth cache
            BasicScheme basicAuth = new BasicScheme();

            clientBuilder.setDefaultRequestConfig(RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .setConnectTimeout(connectionTimeout)
                    .setSocketTimeout(socketTimeout)
                    .setConnectionRequestTimeout(connectionRequestTimeout).build());

            // If there's a proxy server, set it now.
            if(StringUtils.isNotEmpty(proxyHost)){
                httpProxyHost = new HttpHost(proxyHost, proxyPort);
                clientBuilder.setProxy(httpProxyHost);
                basicAuth.processChallenge(new BasicHeader(AUTH.PROXY_AUTH, "BASIC realm=default"));
                authCache.put(httpProxyHost, basicAuth);

            }
            else{
                authCache.put(httpHost, basicAuth);
            }

            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);

            // If authentication was supplied
            if(StringUtils.isNotEmpty(user) && StringUtils.isEmpty(proxyUser)){
                context.setAuthCache(authCache);
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
            }
            else if(StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(proxyUser)){
                context.setAuthCache(authCache);
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
                credentialsProvider.setCredentials(
                        AuthScope.ANY,
                        new UsernamePasswordCredentials(proxyUser, proxyPassword));
            }
            else if (StringUtils.isNotEmpty(proxyUser)){
                context.setAuthCache(authCache);
                credentialsProvider.setCredentials(
                        AuthScope.ANY,
                        new UsernamePasswordCredentials(proxyUser, proxyPassword));
            }

            SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
            sslContextBuilder.loadTrustMaterial(new TrustAllStrategy());
            if(StringUtils.isNotEmpty(pkiKeystore) ) {
                // If they are using a PKI auth set it up now
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(new FileInputStream(pkiKeystore), pkiKeystorePass.toCharArray());
                sslContextBuilder.loadKeyMaterial(ks, pkiKeystorePass.toCharArray());
            }

            SSLContext sslCtx = sslContextBuilder.build();

            SSLConnectionSocketFactory factory = null;
            if (bypassVerify) {
                factory = new SSLConnectionSocketFactory(sslCtx, NoopHostnameVerifier.INSTANCE);
            }
            else{
                factory = new SSLConnectionSocketFactory(sslCtx);
            }
            clientBuilder.setSSLSocketFactory(factory);

            // We need to create a registry for socket factories
            // for both http and https or pooling will not work.
            Registry registry = RegistryBuilder.create()
                    .register("https", factory)
                    .register("http", PlainConnectionSocketFactory.getSocketFactory()).build();
            PoolingHttpClientConnectionManager mgr = new PoolingHttpClientConnectionManager(registry);
            mgr.setDefaultMaxPerRoute(defaultMaxPerRoute);
            mgr.setMaxTotal(maxTotal);
            clientBuilder.setConnectionManager(mgr);

            CloseableHttpClient httpClient = clientBuilder.build();
            RestClient restClient = new RestClient(httpClient, httpHost, context);

            return restClient;
        }
        catch (Exception e){
            logger.error( "Connection setup failed", e);
            throw new RuntimeException("Error establishing http connection for: " + host, e);
        }
    }
}