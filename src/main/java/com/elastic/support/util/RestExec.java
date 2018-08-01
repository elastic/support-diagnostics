package com.elastic.support.util;

import com.elastic.support.diagnostics.Constants;
import org.apache.commons.io.IOUtils;
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
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

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
      String responseString= "";
      boolean completed = false;
      String message = "An error occurred during REST call: " + url + " Check logs for more information. ";

      try {
         response = exec(url);
         int status  = response.getStatusLine().getStatusCode();

         if(status != 200){
            if(status == 401){
               message = message + " Invalid authentication credentials provided.";
            }
            else if(status == 403){
               message = message  + " Insufficient authority to execute query.";
            }
            logger.log(SystemProperties.DIAG, "{} could not be retrieved. Status:{}", url, status);
            logger.log(SystemProperties.DIAG, "{}", getResponseString(response));

         }
         else {
            completed = true;
            responseString = getResponseString(response);
         }

      } catch (Exception e) {
         logger.log(SystemProperties.DIAG, "Exception during REST call", e);
         message = message + e.getMessage();
      } finally {
         HttpClientUtils.closeQuietly(response);
      }

      if(!completed){
         throw new RuntimeException(message);
      }

      return responseString;
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

      try {
         HttpGet httpget = new HttpGet(url);
         if (isSecured) {
            return client.execute(httpHost, httpget, getLocalContext(httpHost));
         } else {
            return client.execute(httpHost, httpget);
         }
      } catch (HttpHostConnectException e) {
         logger.log(SystemProperties.DIAG, "Host connection error.", e);
         throw new RuntimeException("Host connection error: " + e.getMessage() );
      } catch (Exception e) {
         logger.log(SystemProperties.DIAG, "Query Execution Error", e);
         throw new RuntimeException("Query Execution Error: " + e.getMessage());
      }

   }

   protected String getResponseString(HttpResponse response) {

      try {
         HttpEntity entity = response.getEntity();
         return EntityUtils.toString(entity);
      } catch (Exception e) {
         logger.log(SystemProperties.DIAG, "Error while processing response.", e);
         throw new RuntimeException("Response processing could not be completed.");
      }
   }

   protected boolean streamResponseToFile(HttpResponse response, String destination) {

      try {
         int status = checkResponseCode(response);
         org.apache.http.HttpEntity entity = response.getEntity();
         InputStream responseStream = entity.getContent();

         if (status == 200) {
            FileOutputStream fos = new FileOutputStream(destination);
            IOUtils.copy(responseStream, fos);
            logger.log(SystemProperties.DIAG, "File {} was retrieved and saved to disk.", destination);
         }
         else{
            StringWriter writer = new StringWriter();
            String encoding = StandardCharsets.UTF_8.name();
            IOUtils.copy(responseStream, writer, encoding);
            String msg = writer.toString();
            logger.log(SystemProperties.DIAG, "File {} was retrieved and saved to disk.", destination);
            return false;
         }
      } catch (Exception e) {
         logger.error("Error processing response", e);
         if(e.getMessage().equals("400")){
            logger.info("{} will not be written.", destination);
         }

         return false;

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

      if (statusCode == 400) {
         logger.info("No data retrieved.");
      } else if (statusCode == 401) {
         logger.info("Authentication failure: invalid login credentials. Check logs for details.");
      } else if (statusCode == 403) {
         logger.info("Authorization failure or invalid license. Check logs for details.");
      } else if (statusCode == 404) {
         logger.info("Endpoint does not exist.");
      } else if (statusCode > 500 && statusCode < 600) {
         logger.info("Unrecoverable server error.");
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