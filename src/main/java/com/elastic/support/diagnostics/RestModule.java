package com.elastic.support.diagnostics;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class RestModule {


   DiagnosticRequestFactory requestFactory;
   boolean isBypassVerify;

   private final static Logger logger = LoggerFactory.getLogger(RestModule.class);

   public RestModule(DiagnosticRequestFactory requestFactory, boolean isBypassVerify){
      this.requestFactory = requestFactory;
      this.isBypassVerify = isBypassVerify;
   }

   public String submitRequest(String url){

      HttpResponse response = null;
      String result = "";
      try{
         HttpClient client = getClient();
         HttpGet httpget = new HttpGet(url);
         response = client.execute(httpget);
         try {
            org.apache.http.HttpEntity entity = response.getEntity();
            if (entity != null ){
               checkResponseCode(url, response);
               result = EntityUtils.toString(entity);
            }
         }
         catch (Exception e){
            logger.error("Error handling response for " + url, e);
         } finally {
            HttpClientUtils.closeQuietly(response);
         }
      }
      catch (HttpHostConnectException e){
         throw new RuntimeException("Error connecting to host " + url, e);
      }
      catch (Exception e){
         if (e.getMessage().contains("401 Unauthorized")) {
            logger.error("Auth failure", e);
            throw new RuntimeException("Authentication failure: invalid login credentials.", e);
         }
         else {
            logger.error("Diagnostic query: " + url + "failed.", e);
         }
      }

      return result;

   }

   public void submitRequest(String url, String queryName, String destination){

      HttpResponse response = null;
      InputStream responseStream = null;

      try{
         HttpClient client = getClient();
         FileOutputStream fos = new FileOutputStream(destination);
         HttpGet httpget = new HttpGet(url);
         response = client.execute(httpget);
         try {
            org.apache.http.HttpEntity entity = response.getEntity();
            if (entity != null ){
               checkResponseCode(queryName, response);
               responseStream = entity.getContent();
               IOUtils.copy(responseStream, fos);
            }
            else{
               Files.write(Paths.get(destination), ("No results for:" + queryName).getBytes());
            }
         }
         catch (Exception e){
            logger.error("Error writing response for " + queryName + " to disk.", e);
         } finally {
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(client);
         }
      }
      catch (Exception e){
         if (url.contains("_license")) {
            logger.info("There were no licenses installed");
         }
         else if (e.getMessage().contains("401 Unauthorized")) {
            logger.error("Auth failure", e);
            throw new RuntimeException("Authentication failure: invalid login credentials.", e);
         }
         else {
            logger.error("Diagnostic query: " + queryName + "failed.", e);
         }
      }

      logger.info("Diagnostic query: " + queryName + " was retrieved and saved to disk.");

   }

   private void checkResponseCode(String name, HttpResponse response){

      int code = response.getStatusLine().getStatusCode();
      if(code != 200){
         String msg = response.getStatusLine().getReasonPhrase();
         logger.warn(name + " query returned: " + response.getStatusLine().getStatusCode() + "/" + msg);
         if (code == 401) {
            throw new RuntimeException("Authentication failure: invalid login credentials. " + code + "/" + msg);
         }
      }
   }

   private HttpClient getClient(){

      HttpClient client = null;
      try {

         if (isBypassVerify) {
            client = requestFactory.getUnverifiedSslClient();
         } else {
            client = requestFactory.getSslClient();
         }
      }
      catch (Exception e){
         logger.error("Could not create HTTP client.", e);
         throw new RuntimeException(e);
      }

      return client;

   }

}