package com.elastic.support.rest;

import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class ClientBuilder {

   private final Logger logger = LogManager.getLogger(ClientBuilder.class);


   private boolean bypassVerify = false;
   private int requestTimeout = 5000;
   private int connectTimeout = 5000;
   private int socketTimeout = 5000;
   private String pkiCredentials;
   private String pkiPassword;
   private String keyStore;
   private String keyStorePass;
   private HttpClientConnectionManager connectionManager;

   public ClientBuilder (int requestTimeout, int socketTimeout, int connectTimeout) {
      this.requestTimeout = requestTimeout;
      this.socketTimeout = socketTimeout;
      this.connectTimeout = connectTimeout;
   }

   public ClientBuilder setKeyStore(String keyStore) {
      this.keyStore = keyStore;
      return this;
   }

   public ClientBuilder setKeyStorePass(String keyStorePass) {
      this.keyStorePass = keyStorePass;
      return this;
   }

   public ClientBuilder setBypassVerify(boolean bypassVerify){
      this.bypassVerify = bypassVerify;
      return this;
   }

   public ClientBuilder setConnectionManager(HttpClientConnectionManager connectionManager){
      this.connectionManager = connectionManager;
      return this;
   }

   public HttpClient build() {
      try {
         HttpClientBuilder client = HttpClients.custom();
         RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                 .setConnectTimeout(connectTimeout)
                 .setSocketTimeout(socketTimeout)
                 .setConnectionRequestTimeout(requestTimeout);

         // Use the same connection manager, request config timeout settings for both
         client.setDefaultRequestConfig(requestConfigBuilder.build());
         client.setConnectionManager(connectionManager);

         // Start off with a base SSLContext that has a lenient trust strategy
         SSLContextBuilder sslContextBuiilder = new SSLContextBuilder();
         sslContextBuiilder.loadTrustMaterial(new LenientStrategy());

         // Add some additional capabilities if necessary.
         if (StringUtils.isNotEmpty(keyStore)) {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(new FileInputStream(keyStore), keyStorePass.toCharArray());
            sslContextBuiilder.loadKeyMaterial(ks, keyStorePass.toCharArray());
         }

         if (bypassVerify) {
            client.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContextBuiilder.build(), new BypassHostnameVerifier()));
         } else {
            client.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContextBuiilder.build()));
         }

         return client.build();


      } catch (Exception e) {
         logger.log(SystemProperties.DIAG, e.getMessage(), e);
         throw new RuntimeException("Error creating REST client", e);
      }
   }


}

