package com.elastic.support.rest;

import com.elastic.support.BaseConfig;
import com.elastic.support.Constants;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.FileInputStream;
import java.security.KeyStore;

import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;

public class RestClient implements Closeable {

    private static final Logger logger = LogManager.getLogger(RestClient.class);
    private CloseableHttpClient client;
    private HttpHost httpHost;
    private PoolingHttpClientConnectionManager mgr;

    public RestClient(ElasticRestClientInputs inputs, BaseConfig config) {

        this(inputs.url, inputs.bypassAuth, inputs.verifyHost, inputs.user, inputs.password, inputs.pkiKeystore,
                inputs.pkiPass, inputs.proxyUrl, config.connectionTimeout,
                config.connectionRequestTimeout, config.socketTimeout, config.maxTotalConn, config.maxConnPerRoute, config.idleExpire);
    }

    public RestClient(String url, boolean bypassAuth, boolean verifyHost, String user, String password,
            String pkiKeystore, String pkiPass, String proxyUrl,
            int connectionTimeout, int connectionRequestTimeout, int socketTimeout, int maxTotal,
            int defaultMaxPerRoute, long idleExpire) {

        try {
            HttpClientBuilder clientBuilder = HttpClients.custom();
            BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

            RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
            requestConfigBuilder.setCookieSpec(StandardCookieSpec.RELAXED)
                    .setResponseTimeout(Timeout.ofSeconds(socketTimeout))
                    .setConnectTimeout(Timeout.ofSeconds(connectionTimeout))
                    .setConnectionRequestTimeout(Timeout.ofSeconds(connectionRequestTimeout));

            httpHost = HttpHost.create(url);
            if (!bypassAuth && StringUtils.isEmpty(pkiKeystore)) {
                credsProvider.setCredentials(new AuthScope(httpHost),
                        new UsernamePasswordCredentials(user, password.toCharArray()));
            }

            HttpHost proxyHost = null;
            if (StringUtils.isNotEmpty(proxyUrl)) {
                proxyHost = HttpHost.create((proxyUrl));
                requestConfigBuilder.setProxy(proxyHost);
            }

            clientBuilder.evictExpiredConnections()
                    .evictIdleConnections(TimeValue.ofMilliseconds(idleExpire))
                    .setDefaultRequestConfig(requestConfigBuilder.build())
                    .setDefaultCredentialsProvider(credsProvider);

            RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
            registryBuilder.register("http", PlainConnectionSocketFactory.getSocketFactory());

            if (url.contains("https")) {
                SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
                sslContextBuilder.loadTrustMaterial(new TrustAllStrategy());

                if (StringUtils.isNotEmpty(pkiKeystore)) {
                    // If they are using a PKI auth set it up now
                    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                    ks.load(new FileInputStream(pkiKeystore), pkiPass.toCharArray());
                    sslContextBuilder.loadKeyMaterial(ks, pkiPass.toCharArray());
                }

                SSLConnectionSocketFactory factory = null;
                if (verifyHost) {
                    factory = new SSLConnectionSocketFactory(sslContextBuilder.build(), new String[] {"TLSv1.2" }, null,
                            new DefaultHostnameVerifier());
                } else {
                    factory = new SSLConnectionSocketFactory(sslContextBuilder.build(), new String[] {"TLSv1.2" }, null,
                            NoopHostnameVerifier.INSTANCE);
                }

                registryBuilder.register("https", factory);
            }

            mgr = new PoolingHttpClientConnectionManager(registryBuilder.build());
            mgr.setDefaultMaxPerRoute(defaultMaxPerRoute);
            mgr.setMaxTotal(maxTotal);
            mgr.setDefaultSocketConfig(SocketConfig.custom()
                    .setTcpNoDelay(true)
                    .build());
            mgr.setValidateAfterInactivity(TimeValue.ofSeconds(5));

            clientBuilder.setConnectionManager(mgr);
            client = clientBuilder.build();

        } catch (Exception e) {
            logger.error("Connection setup failed", e);
            throw new RuntimeException("Error setting up http connection for: " + url, e);
        }
    }

    public RestResult execQuery(String query) {
        logger.debug("Executing {}.", query);
        BasicClassicHttpRequest req = new BasicClassicHttpRequest(Method.GET, httpHost, query);
        return execRequest(req, new RestResult());
    }

    public RestResult execQuery(String query, String fileName) {
        logger.debug("Executing {} and writing to: {}.", query, fileName);
        BasicClassicHttpRequest req = new BasicClassicHttpRequest(Method.GET, httpHost, query);
        return execRequest(req, new RestResult(fileName));
    }

    private RestResult execRequest(ClassicHttpRequest httpRequest, RestResult result) {
        try {
            return client.execute(httpHost, httpRequest, result);
        } catch (HttpHostConnectException e) {
            logger.error("Host connection error.", e);
            throw new RuntimeException("Host connection");
        } catch (Exception e) {
            logger.error("Unexpected Execution Error", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public RestResult execPost(String url, String payload) {
        try {
            BasicClassicHttpRequest req = new BasicClassicHttpRequest(Method.POST, httpHost, url);
            req.setEntity(new StringEntity(payload));
            req.setHeader("Accept", "application/json");
            req.setHeader("Content-type", "application/json");
            logger.debug(url + SystemProperties.fileSeparator + payload);
            return execRequest(req, new RestResult());
        } catch (Exception e) {
            logger.error(Constants.CONSOLE, "Error with json body.", e);
            throw new RuntimeException("Could not complete post request.");
        }
    }

    public RestResult execDelete(String url) {
        logger.debug(url);
        BasicClassicHttpRequest req = new BasicClassicHttpRequest(Method.DELETE, httpHost, url);
        return execRequest(req, new RestResult());
    }

    public void flushExpired(long idleTimeout){
        mgr.closeExpired();
        mgr.closeIdle(TimeValue.ofMilliseconds(idleTimeout));
    }

    public void close() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            logger.error("Error occurred closing client connection.");
        }
    }

    private HttpClientContext createRequestContext() {
        return null;
    }
}

