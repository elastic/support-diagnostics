package com.elastic.support.diagnostics;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
*/

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RestModule {

/*   RestTemplate restTemplate;
   HttpEntity<String> request;*/

   HttpClient client;

   private final static Logger logger = LoggerFactory.getLogger(RestModule.class);

/*   public RestModule(RestTemplate restTemplate, HttpEntity<String> request) {
      this.restTemplate = restTemplate;
      this.request = request;
   }*/

   public RestModule(HttpClient client){
      this.client = client;
   }

   public String submitRequest(String url){

      HttpResponse response = null;
      String result = "";
      try{
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
            HttpClientUtils.closeQuietly(response);         }
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
            HttpClientUtils.closeQuietly(response);         }
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

/*
   public String submitRequest(String url) {

      String result;
      try {
         ;
         logger.debug("Submitting: " + url);
         ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
         result = response.getBody();
         logger.debug(result);

      } catch (RestClientException e) {
         if (url.contains("_license")) {
            logger.info("There were no licenses installed");
            return "No licenses installed";
         }
         String msg = "Please check log file for additional details.";
         logger.error("Error submitting request\n:", e);
         if (e.getMessage().contains("401 Unauthorized")) {
            msg = "Authentication failure: invalid login credentials.\n" + msg;
         }
         throw new RuntimeException(msg);
      }

      return result;

   }
   */
}