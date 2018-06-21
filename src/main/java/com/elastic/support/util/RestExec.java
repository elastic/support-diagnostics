package com.elastic.support.util;

import com.elastic.support.diagnostics.Constants;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpEntity;
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

public class RestExec {

   ClientBuilder requestFactory;
   boolean isBypassVerify;
   boolean isSecured;
   ClientBuilder clientBuilder = new ClientBuilder();
   HttpClient client = clientBuilder.defaultClient();
   HttpHost httpHost = clientBuilder.getHttpHost();

   private final static Logger logger = LogManager.getLogger(RestExec.class);

   public RestExec setClient(HttpClient client) {
      this.client = client;
      return this;
   }

   public RestExec setHttpHost(HttpHost httpHost) {
      this.httpHost = httpHost;
      return this;
   }

   public RestExec setSecured(boolean isSecured) {
      this.isSecured = isSecured;
      return this;
   }

   public String execBasic(String url) {

      HttpResponse response = null;
      try {
         response = exec(url);
         return getResponseString(response);
      } catch (Exception e) {
         logger.error("Failed to execute request: {}", url);
      } finally {
         HttpClientUtils.closeQuietly(response);
      }

      return "";

   }

   public boolean execConfiguredQuery(String url, String query, String destination) {

      HttpResponse response = null;
      boolean ret = true;
      try {
         response = exec(url);
         streamResponseToFile(response, destination);

      } catch (Exception e) {
         ret = false;
      } finally {
         HttpClientUtils.closeQuietly(response);
      }
      return ret;

   }

   protected HttpResponse exec(String url) {
      String result = "";
      try {
         HttpGet httpget = new HttpGet(url);
         if (isSecured) {
            return client.execute(httpHost, httpget, getLocalContext(httpHost));
         } else {
            return client.execute(httpHost, httpget);
         }
      } catch (HttpHostConnectException e) {
         logger.log(SystemProperties.DIAG, "Host connection error.", e);
         throw new RuntimeException("Error connecting to host " + url, e);
      } catch (Exception e) {
         logger.log(SystemProperties.DIAG, "Error executing query.", e);
         throw new RuntimeException(e);
      }

   }

   protected String getResponseString(HttpResponse response) {

      try {
         HttpEntity entity = response.getEntity();
         int status = checkResponseCode(response);
         return EntityUtils.toString(entity);
      } catch (Exception e) {
         logger.log(SystemProperties.DIAG, "Error while processing response.", e);
         throw new RuntimeException("Response processing could not be completed.");
      }
   }

   protected boolean streamResponseToFile(HttpResponse response, String destination) {

      try {
         int status = checkResponseCode(response);
         if (status == 400) {
            logger.error("No data retrieved. Bypassing write of: {}", destination);
            throw new RuntimeException("No data retrieved.");
         } else if (status == 401) {
            logger.error("Authentication failure: invalid login credentials. Cannot continue.");
            throw new RuntimeException("Authentication failure.");
         } else if (status == 403) {
            logger.error("Authorization failure: invalid login credentials. Cannot continue.");
            throw new RuntimeException("Authorization failure.");
         } else if (status == 404) {
            logger.error("Endpoint does not exist.");
            throw new RuntimeException("No endpoint for URL.");
         } else if (status > 500 && status < 600) {
            logger.error("Unrecoverable server error.");
            throw new RuntimeException("Unrecoverable server error.");
         }
         org.apache.http.HttpEntity entity = response.getEntity();
         InputStream responseStream = entity.getContent();
         FileOutputStream fos = new FileOutputStream(destination);
         IOUtils.copy(responseStream, fos);
         logger.log(SystemProperties.DIAG, "File {} was retrieved and saved to disk.", destination);
      } catch (Exception e) {
         logger.error("Error processing response", e);
         throw new RuntimeException("Failed to process response.");
      }
      return true;
   }

   public int checkResponseCode(HttpResponse response) {

      int statusCode = response.getStatusLine().getStatusCode();
      String reasonPhrase = response.getStatusLine().getReasonPhrase();
      String logMsg = "Http Status Code: {}, Reason: {}";

      if (statusCode != 200) {
         logger.error(logMsg, statusCode, reasonPhrase);
      }

      return statusCode;
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