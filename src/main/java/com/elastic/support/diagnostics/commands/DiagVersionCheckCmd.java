package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.diagnostics.InputParams;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.ClientBuilder;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.RestExec;
import com.elastic.support.util.SystemProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.client.HttpClient;

import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class DiagVersionCheckCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      Map resultMap = null;
      InputParams inputs = context.getInputParams();

      if(inputs.isBypassDiagVerify()){return true;}

      boolean rc = true;

      logger.info("Checking for diagnostic version updates.");

      try {

         String ghHost = (String) context.getConfig().get("diagReleaseHost");
         String ghEndpoint = (String) context.getConfig().get("diagReleaseDest");
         String ghScheme = (String) context.getConfig().get("diagReleaseScheme");
         String diagVersion = context.getStringAttribute(Constants.DIAG_VERSION);
         if (diagVersion.equalsIgnoreCase("debug")) {
            logger.info("Running in debugger - bypassing check");
            return true;
         }

         ClientBuilder cb = new ClientBuilder();
         cb.setConnectTimeout(6000);
         cb.setRequestTimeout(10000);
         cb.setHost(ghHost);
         cb.setScheme(ghScheme);
         HttpClient client = cb.defaultClient();

         RestExec restExec = new RestExec()
            .setClient(client)
            .setHttpHost(cb.getHttpHost());

         String result = null;
         result = restExec.execBasic(ghScheme + "://" + ghHost + "/" + ghEndpoint);

         JsonNode rootNode = JsonYamlUtils.createJsonNodeFromString(result);
         String ver = rootNode.path("tag_name").asText();
         List<JsonNode> assests = rootNode.findValues("assets");
         JsonNode asset = assests.get(0);
         String downloadUrl = asset.path("browser_download_url").asText();

         if (!diagVersion.equals(ver)) {
            logger.info("Warning: Diagnostic version:{} is not the current recommended release", diagVersion);
            logger.info("The current release is {}", ver);
            logger.info("The latest version can be downloaded at {}", downloadUrl);

            Scanner sc = new Scanner(System.in);
            String feedback = null;
            boolean valid = false;
            System.out.println("Continue anyway? [Y/N]");
            feedback = sc.nextLine();
            while (!valid) {
               if ("y".equalsIgnoreCase(feedback) || "n".equalsIgnoreCase(feedback)) {
                  valid = true;
               } else {
                  System.out.println("Continue anyway? [Y/N]");
                  feedback = sc.nextLine();
               }
            }

            if ("n".equalsIgnoreCase(feedback)) {
               System.out.println("Exiting application");
               rc = false;
            }

         }
      } catch (Exception e) {
         logger.log(SystemProperties.DIAG, e);
         logger.info("Issue encountered while checking diagnostic version for updates.");
         logger.info("Failed to get current diagnostic version from Github.");
         logger.info("If Github is not accessible from this environemnt current supported version cannot be confirmed.");
         rc = true;
      }

      return rc;
   }
}
