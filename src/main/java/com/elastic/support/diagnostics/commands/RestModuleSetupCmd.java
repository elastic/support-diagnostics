package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.DiagnosticContext;
import com.elastic.support.diagnostics.DiagnosticRequestFactory;
import com.elastic.support.diagnostics.RestModule;
import org.apache.http.client.HttpClient;
/*import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;*/

public class RestModuleSetupCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      logger.info("Configuring REST endpoint.");

      try {
         // Create an SSL enabled version - it will work for regular HTTP as well.
         // Note that it will function like a browser where you tell it to go ahead and trust an unknown CA
         int connectTimeout = (Integer) context.getConfig().get("connectTimeout");
         int requestTimeout = (Integer) context.getConfig().get("requestTimeout");
         boolean bypassVerify = context.getInputParams().isSkipVerification();

         String user = null, pass = null;
         boolean isSecured = context.getInputParams().isSecured();
         if (isSecured) {
            user = context.getInputParams().getUsername();
            pass = context.getInputParams().getPassword();
         }

         DiagnosticRequestFactory diagnosticRequestFactory = new DiagnosticRequestFactory(connectTimeout, requestTimeout, isSecured, user, pass);
         //RestTemplate restTemplate = new RestTemplate(diagnosticRequestFactory.getSslReqFactory());
         //HttpEntity<String> request = configureAuth(context.getInputParams());
         //RestModule restModule = new RestModule(restTemplate, request);
         HttpClient client = null;
         if (bypassVerify) {
            client = diagnosticRequestFactory.getUnverifiedSslClient();
         } else {
            client = diagnosticRequestFactory.getSslClient();
         }

         RestModule restModule = new RestModule(client);

         context.setRestModule(restModule);
      } catch (Exception e) {
         String errorMsg = "Failed to create REST submission module";
         logger.error(errorMsg, e);
         return false;
      }

      return true;
   }

/*   public HttpEntity<String> configureAuth(InputParams inputs) {

      HttpHeaders headers = new HttpHeaders();

      // If we need authentication
      if (inputs.isSecured()) {
         String plainCreds = inputs.getUsername()
            + ":" + inputs.getPassword();
         byte[] plainCredsBytes = plainCreds.getBytes();
         byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
         String base64Creds = new String(base64CredsBytes);
         headers.add("Authorization", "Basic " + base64Creds);
      }

      return new HttpEntity<>(headers);

   }*/
}
