package com.elastic.support.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.security.KeyStore;

public class ClientBuilder {

   private final Logger logger = LogManager.getLogger(ClientBuilder.class);

   private HttpHost httpHost;
   private boolean isSecured = false;
   private boolean bypassHostnameVerify = false;
   private int requestTimeout = 10000;
   private int connectTimeout = 6000;
   private String user = "";
   private String password = "";
   private int port = 80;
   private String host = "localhost";
   private String scheme = "http";
   private String clientCred;
   private String clientCreedPassword;
   private String trustCred;
   private String trustCredPassword;
   private String proxyHost;
   private String proxyPassword;
   private int proxyPort;

   public HttpHost getHttpHost(){
      return httpHost;
   }

   public boolean isSecured(){
      return isSecured;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public void setHost(String host) {
      this.host = host;
   }

   public void setScheme(String scheme) {
      this.scheme = scheme;
   }

   public ClientBuilder setRequestTimeout(int requestTimeout) {
      this.requestTimeout = requestTimeout;
      return this;
   }

   public ClientBuilder setUser(String user) {
      this.user = user;
      return this;
   }

   public ClientBuilder setPassword(String password) {
      this.password = password;
      return this;
   }

   public ClientBuilder setConnectTimeout(int connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
   }

   public ClientBuilder setBypassHostnameVerify(boolean verified) {
      this.bypassHostnameVerify = verified;
      return this;
   }

   public ClientBuilder setClientCred(String clientCred) {
      this.clientCred = clientCred;
      return this;
   }

   public ClientBuilder setClientCreedPassword(String clientCreedPassword) {
      this.clientCreedPassword = clientCreedPassword;
      return this;
   }

   public ClientBuilder setTrustCred(String trustCred) {
      this.trustCred = trustCred;
      return this;
   }

   public ClientBuilder setTrustCredPassword(String trustCredPassword) {
      this.trustCredPassword = trustCredPassword;
      return this;
   }

   public ClientBuilder setProxyHost(String proxyHost) {
      this.proxyHost = proxyHost;
      return this;
   }

   public ClientBuilder setProxyPort(int proxyPort) {
      this.proxyPort = proxyPort;
      return this;
   }

   public HttpClient defaultClient() {
      if(scheme.equalsIgnoreCase("https")){
         if(port == 80){
            this.port = 443;
         }
      }

      httpHost = new HttpHost(host, port, scheme);
      return HttpClients.createDefault();
   }

   public HttpClient build() {

      try {
         httpHost = new HttpHost(host, port, scheme);
         HttpClientBuilder builder = HttpClients.custom();
         RequestConfig.Builder requestConfig = RequestConfig.custom();
         requestConfig.setConnectTimeout(connectTimeout);
         requestConfig.setSocketTimeout(requestTimeout);
         if (!StringUtils.isEmpty(proxyHost)) {
            requestConfig.setProxy(new HttpHost(proxyHost, proxyPort));
         }

         builder.setDefaultRequestConfig(
            RequestConfig.custom()
               .setConnectTimeout(connectTimeout)
               .setSocketTimeout(requestTimeout).build());

         if (!StringUtils.isEmpty(user) && !StringUtils.isEmpty(password)) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
               new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
               new UsernamePasswordCredentials(user, password));
            builder.setDefaultCredentialsProvider(credentialsProvider);
         } else if (StringUtils.isEmpty(user) && !StringUtils.isEmpty(password)) {
            logger.warn("User: {}  was entered with no password. Bypassing authentication", user);
         } else if (StringUtils.isEmpty(user) && !StringUtils.isEmpty(password)) {
            logger.warn("Password was entered with no user. Bypassing authentication");
         }

         if (bypassHostnameVerify) {
            builder.setSSLHostnameVerifier(new NoopHostnameVerifier());
         }

         SSLContextBuilder sslContextBuiilder = new SSLContextBuilder();
         sslContextBuiilder.loadTrustMaterial(new TrustSelfSignedStrategy());

         if (!StringUtils.isEmpty(clientCred) && !StringUtils.isEmpty(clientCreedPassword)) {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(new FileInputStream(clientCred), clientCreedPassword.toCharArray());
            sslContextBuiilder.loadKeyMaterial(ks, clientCreedPassword.toCharArray());
         } else if (StringUtils.isEmpty(user) && !StringUtils.isEmpty(password)) {
            logger.warn("Client Auth keystore: {} was supplied with no password. Bypassing authentication", clientCred);
         } else if (StringUtils.isEmpty(user) && !StringUtils.isEmpty(password)) {
            logger.warn("Client Auth keystore password was entered with no user. Bypassing authentication");
         }

         builder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContextBuiilder.build()));

         return builder.build();

      } catch (Exception e) {
         logger.error("Error occurred creating SSL Client Request Factory", e);
         throw new RuntimeException("HttpComponentsClientHttpRequestFactory failed to create client instance");
      }

   }

/*  public ClientBuilder(int connectTimeout, int requestTimeout, boolean isSecured,
                        String user, String pass, String keystoreFile, String keystorePass) {
      this.requestTimeout = requestTimeout;
      this.connectTimeout = requestTimeout;
      this.isSecured = isSecured;
      this.keystorePass = keystorePass;

      if (keystoreFile != null && keystorePass != null) {
         try {
            this.keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            this.keystore.load(new FileInputStream(keystoreFile), keystorePass.toCharArray());
            userCert = true;
         } catch (Exception e) {
            logger.error("Error loading input keystore", e);
            throw new RuntimeException("Could not create request context");
         }
      }

      if (isSecured) {
         credentialsProvider.setCredentials(
            new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
            new UsernamePasswordCredentials(user, pass));
      }
   }

   *//**
    * provide SSLContext that allows self-signed or internal CA
    *//*
   public HttpClient getSslClient() {
      CloseableHttpClient httpClient;
      logger.debug("Retrieving SSL HTTP client");
      try {


         RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(connectTimeout)
            .setSocketTimeout(requestTimeout).build();

         if (isSecured && !userCert) {
            httpClient = HttpClients.custom()
               .setDefaultRequestConfig(requestConfig)
               .setDefaultCredentialsProvider(credentialsProvider)
               .setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContexts.custom()
                     .loadTrustMaterial(null, new ShieldDiagnosticStrategy())
                     .build()
                  )
               ).build();
         } else if (isSecured && userCert) {

            httpClient = HttpClients.custom()
               .setDefaultRequestConfig(requestConfig)
               .setDefaultCredentialsProvider(credentialsProvider)
               .setSSLSocketFactory(new SSLConnectionSocketFactory(
                     SSLContexts.custom()
                        .loadTrustMaterial(null, new ShieldDiagnosticStrategy())
                        .loadKeyMaterial(keystore, keystorePass.toCharArray())
                        .build()
                  )
               ).build();

         } else if (!isSecured && userCert) {
            httpClient = HttpClients.custom()
               .setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContexts.custom()
                     .loadTrustMaterial(null, new ShieldDiagnosticStrategy())
                     .loadKeyMaterial(keystore, keystorePass.toCharArray())
                     .build()
                  )
               ).build();
         } else {
            httpClient = HttpClients.custom()
               .setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContexts.custom()
                     .loadTrustMaterial(null, new ShieldDiagnosticStrategy())
                     .build()
                  )
               ).build();
         }
      } catch (Exception e) {
         logger.error("Error occurred creating SSL Client Request Factory", e);
         throw new RuntimeException("HttpComponentsClientHttpRequestFactory failed to create client instance");
      }

      return httpClient;
   }

   public HttpClient getUnverifiedSslClient() {
      HttpClient httpClient;
      logger.info("Retrieving Unverified SSL HTTP client - this is NOT RECOMMENDED");

      try {
         if (isSecured && !userCert) {
            httpClient = HttpClients.custom()
               .setDefaultCredentialsProvider(credentialsProvider)
               .setSSLSocketFactory(new SSLConnectionSocketFactory(
                  SSLContexts.custom().
                     loadTrustMaterial(null, new ShieldDiagnosticStrategy())
                     .build(), new BypassHostnameVerifier()))
               .build();
         } else if (isSecured && userCert) {
            httpClient = HttpClients.custom()
               .setDefaultCredentialsProvider(credentialsProvider)
               .setSSLSocketFactory(new SSLConnectionSocketFactory(
                  SSLContexts.custom()
                     .loadTrustMaterial(null, new ShieldDiagnosticStrategy())
                     .loadKeyMaterial(keystore, keystorePass.toCharArray())
                     .build(), new BypassHostnameVerifier()))
               .build();
         } else if (!isSecured && userCert) {
            httpClient = HttpClients.custom()
               .setSSLSocketFactory(new SSLConnectionSocketFactory(
                  SSLContexts.custom()
                     .loadTrustMaterial(null, new ShieldDiagnosticStrategy())
                     .loadKeyMaterial(keystore, keystorePass.toCharArray())
                     .build(), new BypassHostnameVerifier()))
               .build();
         } else {
            httpClient = HttpClients.custom()
               .setSSLSocketFactory(new SSLConnectionSocketFactory(
                  SSLContexts.custom().
                     loadTrustMaterial(null, new ShieldDiagnosticStrategy())
                     .build(), new BypassHostnameVerifier()))
               .build();
         }
      } catch (Exception e) {
         logger.error("Error occurred creating SSL Client Request Factory", e);
         throw new RuntimeException("HttpComponentsClientHttpRequestFactory failed to create client instance");
      }
      return httpClient;
   }

   private class ShieldDiagnosticStrategy extends TrustSelfSignedStrategy {

      public ShieldDiagnosticStrategy() {
         super();
      }

      *//**
    * This instructs the client not to care if the cert it's getting isn't in the
    * truststore.  This is more lenient than the TrustSelfSignedStrategy it's inherited from
    * because when retrieving remotely the TrustSelfSignedStrategy will only bypass self signed, not
    * those from an internal CA that hasn't been set up on the client side truststore.  Essentially,
    * it's bacause internal CA != self-signed.  So you get an error on internal CA's that haven't been
    * installed.
    *
    * @param chain
    * @param authType
    * @return true.  always.
    *//*
      @Override
      public boolean isTrusted(X509Certificate[] chain, String authType) {
         return true;
      }
   }

   *//**
    * This overrides any hostname mismatch in the certificate
    *//*
   private class BypassHostnameVerifier implements HostnameVerifier {

      public boolean verify(String hostname, SSLSession session) {
         return true;
      }

   }*/

}

