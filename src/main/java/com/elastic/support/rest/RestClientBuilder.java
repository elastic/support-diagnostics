package com.elastic.support.rest;

import com.elastic.support.config.Constants;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import java.io.File;

import java.io.FileInputStream;
import java.security.KeyStore;


public class RestClientBuilder {

    private final Logger logger = LogManager.getLogger(RestClientBuilder.class);

    private  PoolingHttpClientConnectionManager connectionManager;
    HttpClientBuilder clientBuilder = HttpClients.custom();

    private int connectionRequestTimeout = 10 * 1000;
    private int socketTimeout = 5 * 1000;
    private int connectionTimeout = 5 * 1000;
    private String host = "127.0.0.1", scheme = "http", user, password;
    private String proxyHost, proxyUser, proxyPassword;
    private int port = Constants.DEEFAULT_HTTP_PORT, proxyPort;
    private String pkiKeystore, pkiKeystorePass;
    private boolean bypassVerify = false;
    private File pkiKeystoreFile;
    private boolean poolConnections = false;
    private int maxTotal = 100, defaultMaxPerRoute = 10;

    public PoolingHttpClientConnectionManager getConnectionManager(PoolingHttpClientConnectionManager connectionManager) {
        return this.connectionManager;
    }

    public RestClientBuilder setRequestTimeout(int requestTimeout) {
        this.connectionRequestTimeout = requestTimeout;
        return this;
    }

    public RestClientBuilder setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public RestClientBuilder setConnectTimeout(int connectTimeout) {
        this.connectionTimeout = connectTimeout;
        return this;
    }

    public RestClientBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    public RestClientBuilder setUser(String user) {
        this.user = user;
        return this;
    }

    public RestClientBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public RestClientBuilder setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
        return this;
    }

    public RestClientBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public RestClientBuilder setScheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    public RestClientBuilder setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        return this;
    }

    public RestClientBuilder setProxPort(int proxyPort) {
        this.proxyPort = proxyPort;
        return this;
    }

    public RestClientBuilder setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
        return this;
    }

    public RestClientBuilder setProxyPass(String proxyPassword) {
        this.proxyPassword = proxyPassword;
        return this;
    }

    public RestClientBuilder setPooledConnections(boolean poolConnections){
        this.poolConnections = poolConnections;
        return this;
    }

    public RestClientBuilder setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
        return this;
    }

    public RestClientBuilder setDefaultMaxPerRoute(int defaultMaxPerRoute) {
        this.defaultMaxPerRoute = defaultMaxPerRoute;
        return this;
    }

    public RestClientBuilder setPkiKeystore(String pkiKeystore) {

        pkiKeystoreFile = new File(pkiKeystore);

        // If they specified an invalid input keytstore error it out now since it won't work.
        if (! pkiKeystoreFile.exists()) {
            this.pkiKeystore = pkiKeystore;
        } else {
            logger.error("Invalid PKI authorizetion store specified");
            throw new IllegalArgumentException("Java keystore {} could not be located on the system.");
        }

        return this;
    }

    public RestClientBuilder setPkiKeystorePass(String pkiKeystorePass) {
        this.pkiKeystorePass = pkiKeystorePass;
        return this;
    }

    public RestClientBuilder setBypassVerify(boolean bypassVerify) {
        this.bypassVerify = bypassVerify;
        return this;
    }

    public RestClient build() {

        HttpClientBuilder clientBuilder = HttpClients.custom();

        // Set up is the target destination.
        HttpHost httpHost = new HttpHost(host, port, scheme);

        // Create a new HttpContext to send into the client. This will maintain state that is consistent
        // between subsequent calls to the same destination.
        HttpClientContext httpContext = HttpClientContext.create();

        try{
            SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
            sslContextBuilder.loadTrustMaterial(new TrustAllStrategy());
            SSLContext sslCtx = sslContextBuilder.build();
            SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslCtx);
            clientBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslCtx));

            // If and when we start making connections to multinple nodes this will
            // need to be turned on. Note that we need to create a regsistry for socket factories
            // for both http and https or pooling will not work.
            if(poolConnections) {
                Registry registry = RegistryBuilder.create()
                        .register("https", factory)
                        .register("http", PlainConnectionSocketFactory.getSocketFactory()).build();
                PoolingHttpClientConnectionManager mgr = new PoolingHttpClientConnectionManager(registry);
                mgr.setDefaultMaxPerRoute(defaultMaxPerRoute);
                mgr.setMaxTotal(maxTotal);
                clientBuilder.setConnectionManager(mgr);
            }

            if(StringUtils.isNotEmpty(pkiKeystore) ) {
                // If they are using a PKI auth set it up now
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(new FileInputStream(pkiKeystoreFile), pkiKeystorePass.toCharArray());
                sslContextBuilder.loadKeyMaterial(ks, pkiKeystorePass.toCharArray());
            }

            if (bypassVerify) {
                clientBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslCtx, new NoopHostnameVerifier()));
            }
            else{
                clientBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslCtx));
            }
        }
        catch (Exception e){
            logger.log(SystemProperties.DIAG, "Connection setup failed", e);
            throw new RuntimeException("Error establishing http connection for: " + host);
        }

        // Set up the default request config
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setSocketTimeout(socketTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout);
        clientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());

        boolean isAuth = false;

        // Given most installations will have security there's not much downslide to
        // just creating these by default.
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        // If we have a proxy set it up along with possible authentication credentials.
        if(StringUtils.isNotEmpty(proxyHost)){
            requestConfigBuilder.setProxy(new HttpHost(proxyHost, proxyPort));
            if(StringUtils.isNotEmpty(proxyUser)){
                credentialsProvider.setCredentials(
                        new AuthScope(new HttpHost(proxyHost, proxyPort)),
                        new UsernamePasswordCredentials(proxyUser, proxyPassword));
                isAuth = true;
            }
        }

        // If authentication was supplied
        if(StringUtils.isNotEmpty(user)){
            credentialsProvider.setCredentials(new AuthScope(new HttpHost(host, port, scheme)), new UsernamePasswordCredentials(user, password));
            isAuth = true;
        }

        // We only need to do this if there's auth involved
        if(isAuth){
            AuthCache authCache = new BasicAuthCache();
            authCache.put(httpHost, new BasicScheme());
            httpContext.setAuthCache(authCache);
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }

        CloseableHttpClient httpClient = clientBuilder.build();
        RestClient restClient = new RestClient(httpClient, httpContext, httpHost);
        return restClient;

    }


}

