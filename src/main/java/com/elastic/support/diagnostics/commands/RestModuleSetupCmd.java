package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.ClientBuilder;
import com.elastic.support.util.RestExec;
import org.apache.http.client.HttpClient;

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

         String keystore = context.getInputParams().getKeystore();
         String keystorePass = context.getInputParams().getKeystorePass();

         ClientBuilder cb = new ClientBuilder();
         cb.setBypassHostnameVerify(bypassVerify);
         cb.setUser(user);
         cb.setPassword(pass);
         cb.setConnectTimeout(connectTimeout);
         cb.setRequestTimeout(requestTimeout);
         cb.setPkiCredentials(keystore);
         cb.setPkiPassword(keystorePass);
         cb.setHost(context.getInputParams().getHost());
         cb.setPort(context.getInputParams().getPort());
         cb.setScheme(context.getInputParams().getProtocol());

         HttpClient client = cb.build();

         RestExec restExec = new RestExec()
            .setClient(client)
            .setHttpHost(cb.getHttpHost())
            .setSecured(context.getInputParams().isSecured());
         context.setRestExec(restExec);
      } catch (Exception e) {
         String errorMsg = "Failed to create REST submission module";
         logger.error(errorMsg, e);
         return false;
      }

      return true;
   }

}
