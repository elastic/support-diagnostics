package com.elastic.support.rest;

import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.FileInputStream;
import java.security.KeyStore;


public class RestClientBuilder {

    private final Logger logger = LogManager.getLogger(RestClientBuilder.class);
    private HttpClientBuilder clientBuilder = HttpClients.custom();
    private RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
    private SSLContextBuilder sslContextBuiilder = new SSLContextBuilder();
    private CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

    public RestClientBuilder(){
        try {
            // Start off with a base SSLContext that has a lenient trust strategy
            sslContextBuiilder.loadTrustMaterial(new LenientStrategy());
            LayeredConnectionSocketFactory lcsf = new SSLConnectionSocketFactory(sslContextBuiilder.build());
            clientBuilder.setSSLSocketFactory(lcsf);

        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, e.getMessage(), e);
            throw new RuntimeException("Error creating REST client");
        }
    }

    public RestClientBuilder setClientTimeouts(int requestTimeout, int socketTimeout, int connectTimeout) {
        requestConfigBuilder.setConnectTimeout(connectTimeout);
        requestConfigBuilder.setSocketTimeout(socketTimeout);
        requestConfigBuilder.setConnectTimeout(connectTimeout);
        return this;
    }

    public RestClientBuilder setProxy(String proxyHost, int proxyPort) {
        HttpHost proxy = new HttpHost(proxyHost, proxyPort);
        requestConfigBuilder.setProxy(proxy);
        return this;
    }

    public RestClientBuilder setProxyAuth(String proxyHost, int proxyPort, String proxyUser, String proxyPassword) {
        credentialsProvider.setCredentials(
                new AuthScope(proxyHost, proxyPort),
                new UsernamePasswordCredentials(proxyUser, proxyPassword));
        return this;
    }

    public RestClientBuilder setPkiKeystore(String pkiKeyStore, String pkiKeyStorePass) {

        try(FileInputStream fs = new FileInputStream(pkiKeyStore)) {

            if (StringUtils.isNotEmpty(pkiKeyStore)) {
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(fs, pkiKeyStorePass.toCharArray());
                sslContextBuiilder.loadKeyMaterial(ks, pkiKeyStorePass.toCharArray());
            }
        }
        catch (Exception e){
            logger.log(SystemProperties.DIAG, e.getMessage(), e);
            throw new RuntimeException("Error creating REST client PKI store");
        }

        return this;
    }

    public RestClientBuilder setBypassVerify(boolean bypassVerify) {
        try {
            if(bypassVerify) {
                clientBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContextBuiilder.build(), new BypassHostnameVerifier()));
            }
        } catch ( Exception e){
            logger.log(SystemProperties.DIAG, e.getMessage(), e);
            throw new RuntimeException("Error creating REST client Bypass Verification");
        }
        return this;
    }

    public RestClientBuilder setConnectionManager(HttpClientConnectionManager connectionManager) {
        clientBuilder.setConnectionManager(connectionManager);
        return this;
    }


    public RestClient build() {

        RequestConfig requestConfig = requestConfigBuilder.build();
        clientBuilder.setDefaultRequestConfig(requestConfig);
        return new RestClient(clientBuilder.build(), credentialsProvider);

    }


}

