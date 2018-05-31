package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.diagnostics.InputParams;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.RestModule;
import com.elastic.support.util.SystemUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;


public class DiagVersionCheckCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      Map resultMap = null;
      InputParams inputs = context.getInputParams();
      boolean rc = true;

      logger.info("Checking for diagnostic version updates.");

      try {

         String ghHost = (String) context.getConfig().get("diagReleaseHost");
         String ghEndpoint = (String) context.getConfig().get("diagReleaseDest");
         String diagVersion = context.getStringAttribute(Constants.DIAG_VERSION);
         if(diagVersion.equalsIgnoreCase("debug")){
            logger.info("Running in debugger - bypassing check");
            return true;
         }

         RestModule restModule = context.getRestModule();
         String result = restModule.submitRequest("https", ghHost, 80, ghEndpoint);

         JsonNode rootNode = JsonYamlUtils.createJsonNodeFromString(result);
         if(! rootNode.isArray()){
            logger.error("Invalid response when checking Github releases - bypassing.", result);
            return true;
         }

         String ver = rootNode.path("tag_name").asText();
         List<JsonNode> assests = rootNode.findValues("assets");
         JsonNode asset = assests.get(0);
         String downloadUrl = asset.path("browser_download_url").asText();

         if(! diagVersion.equals(ver)){
            logger.warn("Warning: Diagnostic version:{} is not the most recent release", diagVersion);
            logger.warn("The current release is {}", ver);
            logger.warn("The latest version can be downloaded at {}", downloadUrl );

            Scanner sc = new Scanner(System.in);
            System.out.println("Continue anyway?[Y/N]");
            if(! sc.nextLine().equalsIgnoreCase("y")){
               System.out.println("Exiting application");
               rc = false;
            }
         }
      } catch (Exception e) {
         logger.error("Error while checking diagnostic version for updates.", e);
         rc = true;
      }

      return rc;
   }
}
