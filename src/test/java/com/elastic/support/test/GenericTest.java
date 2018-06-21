package com.elastic.support.test;

import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.security.cert.X509Certificate;


public class GenericTest {

    private final Logger logger = LoggerFactory.getLogger(GenericTest.class);

    @Test
    public void testHttps() {

       try {
/*          CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
          credentialsProvider.setCredentials(
             new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
             new UsernamePasswordCredentials("elastic", "elastic"));

          HttpClient client = HttpClients.custom()
             .setDefaultCredentialsProvider(credentialsProvider)
             .setDefaultRequestConfig(
                RequestConfig.custom()
                   .setConnectTimeout(10000)
                   .setSocketTimeout(30000)
                .build()
             )
             .setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContexts.custom()
                   .loadTrustMaterial(null, new ShieldDiagnosticStrategy())
                .build(), new BypassHostnameVerifier())
             ).build();

          HttpGet httpget = new HttpGet("https://192.168.5.175:9201");
          HttpHost httpHost = new HttpHost("192.168.5.175", 9201, "https");
          AuthCache authCache = new BasicAuthCache();
          BasicScheme basicAuth = new BasicScheme();
          authCache.put(httpHost, basicAuth);

          HttpClientContext localContext = HttpClientContext.create();
          localContext.setAuthCache(authCache);

          HttpResponse response =
             client.execute(httpHost, httpget, localContext);

          logger.error("Response", response.getStatusLine());*/

         String url = "https://192.168.5.175:9201";

          int posScheme = url.indexOf("/");
          String scheme = url.substring(0, posScheme).replace(":", "");
          int posPort = url.indexOf(":", posScheme );
          String host = url.substring(posScheme + 2, posPort);
          String port = url.substring(posPort + 1).replace("/", "");
          logger.error(port);



       } catch (Exception e) {
          logger.error("Bad stuff", e);
       }



    }

   private class ShieldDiagnosticStrategy extends TrustSelfSignedStrategy {

      public ShieldDiagnosticStrategy() {
         super();
      }

      public boolean isTrusted(X509Certificate[] chain, String authType) {
         return true;
      }
   }

   private class BypassHostnameVerifier implements HostnameVerifier {

      public boolean verify(String hostname, SSLSession session){
         return true;
      }

   }

}
