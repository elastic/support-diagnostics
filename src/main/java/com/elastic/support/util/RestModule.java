package com.elastic.support.util;

import com.elastic.support.diagnostics.Constants;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RestModule {

   DiagnosticRequestFactory requestFactory;
   boolean isBypassVerify;
   HttpClient client;

   private final static Logger logger = LogManager.getLogger(RestModule.class);

   public RestModule(DiagnosticRequestFactory requestFactory, boolean isBypassVerify) {
      this.requestFactory = requestFactory;
      this.isBypassVerify = isBypassVerify;
      this.client = this.getClient();
   }

   public String submitRequest(String protocol, String host, int port, String request) {

      return submitRequest(protocol, host, port, request, null, null);

   }

   public String submitRequest(String protocol, String host, int port, String request, String queryName, String destination) {

      HttpResponse response = null;
      InputStream responseStream = null;
      String result = null;
      String url = protocol + "://" + host + ":" + port + "/" + request;

      try {
         HttpHost httpHost = new HttpHost(host, port, protocol);
         HttpGet httpget = new HttpGet(url);
         response = client.execute(httpHost, httpget, getLocalContext(httpHost));
         try {
            org.apache.http.HttpEntity entity = response.getEntity();
            if (entity != null) {
               if(queryName == null || queryName.equals("")){
                  checkResponseCode(url, response);
                  result = EntityUtils.toString(entity);
               }
               else{
                  checkResponseCode(queryName, response);
                  responseStream = entity.getContent();
                  FileOutputStream fos = new FileOutputStream(destination);
                  IOUtils.copy(responseStream, fos);
                  logger.info("Diagnostic query: " + queryName + " was retrieved and saved to disk.");
               }
            } else {
               Files.write(Paths.get(destination), ("No results for:" + queryName).getBytes());
            }
         } catch (Exception e) {
            logger.error("Error writing response for " + queryName + " to disk.", e);
         } finally {
            HttpClientUtils.closeQuietly(response);
         }
      } catch (HttpHostConnectException e) {
         throw new RuntimeException("Error connecting to host " + url, e);
      } catch (Exception e) {
         if (request.contains("_license")) {
            logger.info("There were no licenses installed");
         } else if (e.getMessage().contains("401 Unauthorized")) {
            logger.error("Auth failure", e);
            throw new RuntimeException("Authentication failure: invalid login credentials.", e);
         } else {
            logger.error("Diagnostic query: " + queryName + "failed.", e);
         }
      }

      return result;

   }

   public void checkResponseCode(String name, HttpResponse response) {

      int code = response.getStatusLine().getStatusCode();
      if (code != 200) {
         String msg = response.getStatusLine().getReasonPhrase();
         logger.warn(name + " query returned: " + response.getStatusLine().getStatusCode() + "/" + msg);
         if (code == 401) {
            throw new RuntimeException("Authentication failure: invalid login credentials. " + code + "/" + msg);
         }
      }
   }

   public HttpClientContext getLocalContext(HttpHost httpHost) throws Exception {
      AuthCache authCache = new BasicAuthCache();
      // Generate BASIC scheme object and add it to the local
      // auth cache
      BasicScheme basicAuth = new BasicScheme();
      authCache.put(httpHost, basicAuth);

      HttpClientContext localContext = HttpClientContext.create();
      localContext.setAuthCache(authCache);

      return localContext;
   }

   public HttpClient getClient() {

      HttpClient client = null;
      try {
         if (isBypassVerify) {
            client = requestFactory.getUnverifiedSslClient();
         } else {
            client = requestFactory.getSslClient();
         }
      } catch (Exception e) {
         logger.error("Could not create HTTP client.", e);
         throw new RuntimeException(e);
      }

      return client;

   }

   private HttpRequest createRequest(int requestType, String uri) {

      switch (requestType) {
         case Constants.HTTP_DELETE:
            return new HttpDelete(uri);
         case Constants.HTTP_GET:
            return new HttpGet(uri);
         case Constants.HTTP_POST:
            return new HttpPost(uri);
         case Constants.HTTP_PUT:
            return new HttpPut(uri);
      }

      return null;

   }

}