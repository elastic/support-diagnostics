package com.elastic.support.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

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
   private String pkiCredentials;
   private String pkiPassword;
   private String trustCred;
   private String trustCredPassword;
   private String proxyHost;
   private String proxyPassword;
   private int proxyPort;

   public HttpHost getHttpHost() {
      return httpHost;
   }

   public boolean isSecured() {
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

   public String getPkiCredentials() {
      return pkiCredentials;
   }

   public void setPkiCredentials(String pkiCredentials) {
      this.pkiCredentials = pkiCredentials;
   }

   public String getPkiPassword() {
      return pkiPassword;
   }

   public void setPkiPassword(String pkiPassword) {
      this.pkiPassword = pkiPassword;
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
      if (scheme.equalsIgnoreCase("https")) {
         if (port == 80) {
            this.port = 443;
         }
      }

      httpHost = new HttpHost(host, port, scheme);
      return HttpClients.createDefault();
   }

   public HttpClient build() {
      HttpClient httpClient = null;
      try {
         httpHost = new HttpHost(host, port, scheme);
         HttpClientBuilder builder = HttpClients.custom();

         RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
            .setConnectTimeout(connectTimeout)
            .setSocketTimeout(requestTimeout);

         if (!StringUtils.isEmpty(proxyHost)) {
            requestConfigBuilder.setProxy(new HttpHost(proxyHost, proxyPort));
         }

         builder.setDefaultRequestConfig(requestConfigBuilder.build());

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

         SSLContextBuilder sslContextBuiilder = new SSLContextBuilder();

         sslContextBuiilder.loadTrustMaterial(new LenientStrategy());

         if (!StringUtils.isEmpty(pkiCredentials) && !StringUtils.isEmpty(pkiPassword)) {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(new FileInputStream(pkiCredentials), pkiPassword.toCharArray());
            sslContextBuiilder.loadKeyMaterial(ks, pkiPassword.toCharArray());
         } else if (StringUtils.isEmpty(pkiCredentials) && !StringUtils.isEmpty(pkiPassword)) {
            logger.warn("Client Auth keystore: {} was supplied with no password. Bypassing authentication", pkiCredentials);
         } else if (StringUtils.isEmpty(pkiCredentials) && !StringUtils.isEmpty(pkiPassword)) {
            logger.warn("Client Auth keystore password was entered with no store. Bypassing authentication");
         }

         if (bypassHostnameVerify) {
            builder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContextBuiilder.build(), new BypassHostnameVerifier()));
         }
         else{
            builder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContextBuiilder.build()));
         }

         httpClient = builder.build();

      } catch (Exception e) {
         logger.error("Error occurred creating SSL Client Request Factory", e);
         throw new RuntimeException("HttpComponentsClientHttpRequestFactory failed to create client instance");
      }

      return httpClient;

   }


   private class LenientStrategy extends TrustSelfSignedStrategy {

      public LenientStrategy() {
         super();
      }

      public boolean isTrusted(X509Certificate[] chain, String authType) {
         return true;
      }
   }

   /**
    * This overrides any hostname mismatch in the certificate
    */
   private class BypassHostnameVerifier implements HostnameVerifier {

      public boolean verify(String hostname, SSLSession session) {
         return true;
      }

   }

}

