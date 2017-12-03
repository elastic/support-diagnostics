package com.elastic.support.util;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class DiagnosticRequestFactory {

   private final Logger logger = LogManager.getLogger(DiagnosticRequestFactory.class);

   private Integer connectTimeout;
   private Integer requestTimeout;
   private boolean isSecured;
   KeyStore keystore;
   private String keystorePass;
   private boolean userCert;
   CredentialsProvider credentialsProvider  = new BasicCredentialsProvider();

   public DiagnosticRequestFactory(int connectTimeout, int requestTimeout, boolean isSecured,
                                   String user, String pass, String keystoreFile, String keystorePass) {
      this.requestTimeout = requestTimeout;
      this.connectTimeout = requestTimeout;
      this.isSecured = isSecured;
      this.keystorePass = keystorePass;

      if(keystoreFile != null && keystorePass != null){
         try {
            this.keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            this.keystore.load(new FileInputStream(keystoreFile), keystorePass.toCharArray());
            userCert = true;
         } catch (Exception e) {
            logger.error("Error loading input keystore", e);
            throw new RuntimeException("Could not create request context");
         }
      }

      if (isSecured){
         credentialsProvider.setCredentials(
            new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
            new UsernamePasswordCredentials(user, pass));
      }
   }

   /**
    * provide SSLContext that allows self-signed or internal CA
    */
   public HttpClient getSslClient() {
      CloseableHttpClient httpClient;
      logger.debug("Retrieving SSL HTTP client");
      try {

         RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(connectTimeout)
            .setSocketTimeout(requestTimeout).build();

         if(isSecured && !userCert){
            httpClient = HttpClients.custom()
               .setDefaultRequestConfig(requestConfig)
               .setDefaultCredentialsProvider(credentialsProvider)
               .setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContexts.custom()
                     .loadTrustMaterial(null, new ShieldDiagnosticStrategy())
                     .build()
                  )
               ).build();
         }
         else if(isSecured && userCert){

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

         }
         else if (!isSecured && userCert){
            httpClient = HttpClients.custom()
               .setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContexts.custom()
                  .loadTrustMaterial(null, new ShieldDiagnosticStrategy())
                  .loadKeyMaterial(keystore, keystorePass.toCharArray())
                  .build()
                  )
               ).build();         }
         else {
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
         if(isSecured && !userCert){
            httpClient = HttpClients.custom()
               .setDefaultCredentialsProvider(credentialsProvider)
               .setSSLSocketFactory(new SSLConnectionSocketFactory(
                  SSLContexts.custom().
                     loadTrustMaterial(null, new ShieldDiagnosticStrategy())
                     .build(), new BypassHostnameVerifier()))
               .build();
         }
         else if (isSecured && userCert){
            httpClient = HttpClients.custom()
               .setDefaultCredentialsProvider(credentialsProvider)
               .setSSLSocketFactory(new SSLConnectionSocketFactory(
                  SSLContexts.custom()
                     .loadTrustMaterial(null, new ShieldDiagnosticStrategy())
                     .loadKeyMaterial(keystore, keystorePass.toCharArray())
                     .build(), new BypassHostnameVerifier()))
               .build();
         }
         else if (!isSecured && userCert){
            httpClient = HttpClients.custom()
               .setSSLSocketFactory(new SSLConnectionSocketFactory(
                  SSLContexts.custom()
                     .loadTrustMaterial(null, new ShieldDiagnosticStrategy())
                     .loadKeyMaterial(keystore, keystorePass.toCharArray())
                     .build(), new BypassHostnameVerifier()))
               .build();
         }
         else {
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

      /**
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
       */
      @Override
      public boolean isTrusted(X509Certificate[] chain, String authType) {
         return true;
      }
   }

   /**
    * This overrides any hostname mismatch in the certificate
    */
   private class BypassHostnameVerifier implements HostnameVerifier{

      public boolean verify(String hostname, SSLSession session){
         return true;
      }

   }

}

