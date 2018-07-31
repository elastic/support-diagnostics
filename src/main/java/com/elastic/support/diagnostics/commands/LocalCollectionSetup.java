package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class LocalCollectionSetup extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {
      String logDir = "";
      boolean done = false;

      try {
         if (context.getInputParams().isNoLogs()) {
            context.setLogDir(Constants.NOT_FOUND);
            return true;
         }

         Map config = context.getConfig();
         logger.info("Checking default log locations...");

         List<String> defaultLogLocations = (List<String>) config.get("default-log-locations");
         for (String loc : defaultLogLocations) {
            if (isValidLogDir(loc)) {
               logDir = loc;
               break;
            }
         }

         Scanner sc = new Scanner(System.in);
         String answer = "";

         if (StringUtils.isEmpty(logDir)) {
            System.out.println("Could not locate a non-empty log directory at the default location.");
         } else {
            System.out.println("The default Elasticsearch log directory at " + logDir + " exists and appears to have content. Use this?[y/n] ");
            String useDefault = sc.nextLine();
            if (useDefault.equalsIgnoreCase("y")) {
               done = true;
            }
         }

         do {
            answer = promptForDirCollectionOptions(sc);
            if (answer.contains("1")) {
               logDir = getManuallyEnteredLogDir(sc);
               if (!logDir.equals(Constants.NOT_FOUND)) {
                  logger.info("Using supplied directory: {}", logDir);
                  done = true;
               }
            } else if (answer.contains("2")) {
               logDir = getLogDirFromCluster(sc);
               if (!logDir.equals(Constants.NOT_FOUND)) {
                  logger.info("Using directory: {} obtained from cluster info.", logDir);
                  done = true;
               }
            } else {
               System.out.println("No logging directory supplied - continuing but logs will not be collected.");
               context.setLogDir(Constants.NOT_FOUND);
            }
         } while (!done);

      } catch (Exception e) {
         logger.error("Problems encountered while searching for log locations. Proceeding with log collection.");
         logger.log(SystemProperties.DIAG, "Error getting log locations.", e);
         logDir = Constants.NOT_FOUND;
      }

      context.setLogDir(logDir);

      return true;

   }

   private String promptForDirCollectionOptions(Scanner sc) {

      boolean done = false;
      String answer = "";
      do {
         System.out.println("Enter the option number to use:");
         System.out.println("1   Manually enter a log directory path.");
         System.out.println("2   Enter a url that points to a running node in the cluster - the diagnostic will use those to check for locations.");
         System.out.println("3   Don't collect logs at this time.");
         System.out.println("Enter: 1,2, or 3:  ");
         answer = sc.nextLine();
         if (answer.equals("1") || answer.equals("2") || answer.equals("3")) {
            done = true;
         } else {
            System.out.println("Please enter a valid option number.");
         }
      } while (done == false);

      return answer;
   }

   private String getManuallyEnteredLogDir(Scanner sc) {
      boolean done = false;
      String dir = "";

      do {
         System.out.println("Enter the absolute path of the log directory on this host. ");
         dir = sc.nextLine();
         if (isValidLogDir(dir)) {
            done = true;
         } else {
            System.out.println("Invalid directory or it contained no logs. Hit Enter to continue or q to quit. ");
            String resp = SystemUtils.toString(sc.nextLine(), "q");
            if (resp.trim().equalsIgnoreCase("q")) {
               dir = Constants.NOT_FOUND;
               done = true;
            }
         }
      } while (!done);

      return dir;

   }

   private String getLogDirFromCluster(Scanner sc) {
      boolean done = false;
      List<String> components = new ArrayList<>();
      String url = "";

      try {
         do {
            System.out.println("Enter a full URL for a running node in the cluster: ");
            System.out.println("It should be in the format: [http,https]://[host or ip address]:port.");
            url = sc.nextLine();
            components = processInputUrl(url);
            if (components.size() == 0) {
               System.out.println("This does not appear to be a valid URL. Please check your syntax and values.");
               System.out.println("Hit Enter to continue or q to quit.");
               String resp = sc.nextLine();
               if ("q".equalsIgnoreCase(resp)) {
                  throw new RuntimeException("Invalid URL entered: " + url);
               } else {
                  continue;
               }
            }

            System.out.println("Enter a login id if secured or enter to continue: ");
            String user = sc.nextLine();
            components.add(user);

            System.out.println("Enter a password if secured or enter to continue: ");
            String password = sc.nextLine();
            components.add(password);

            done = true;

         } while (!done);

         RestExec restExec = setupClient(components);

         return queryClusterForLogLocations(url, restExec);

      } catch (RuntimeException e) {
         logger.info("An error occurred while using: " + url);
         logger.log(SystemProperties.DIAG, "url: {} failed.", url, e);
      }

      return Constants.NOT_FOUND;

   }

   private boolean isValidLogDir(String dirName) {
      File logdir = new File(dirName);
      if (logdir.exists()) {
         if (logdir.isDirectory()) {
            if (logdir.listFiles().length > 0) {
               return true;
            }
         }
      }
      return false;

   }


   private String queryClusterForLogLocations(String url, RestExec restExec) {
      String logDir;
      List<String> logList = new ArrayList<>();
      System.out.println("Trying REST Endpoint for node logging info: " + url);

      try {
         String nodeResult = restExec.execBasic(url + "/_nodes?pretty");
         JsonNode rootNode = JsonYamlUtils.createJsonNodeFromString(nodeResult);

         Set<String> logDirList = new HashSet();
         JsonNode nodes = rootNode.path("nodes");
         Iterator<JsonNode> it = nodes.iterator();
         while (it.hasNext()) {
            JsonNode node = it.next();
            String dir = node.path("settings").path("path").path("logs").asText();
            logDirList.add(dir);
         }

         for (String dir : logDirList) {
            if (isValidLogDir(dir)) {
               logList.add(dir);
            }
         }

         if (logList.size() > 1) {
            logDir = logList.get(0);
            logger.info("Located log directories...");
            for(String logdir: logList){
               logger.info(logdir);
            }
            logger.info("More that one configured directory found contained content - using first one {}", logList.get(0));
         } else if (logList.size() == 0) {
            logger.info("No valid logging content found at any retrieved location. Specify the directory manually or bypass log collection.");
            logDir = Constants.NOT_FOUND;
         } else {
            logDir = logList.get(0);
         }

      } catch (Exception e) {
         logger.log(SystemProperties.DIAG, "Error occurred while interrogating cluster for possible log locations.");
         throw new RuntimeException("Error getting cluster log configurations.");
      }

      return logDir;

   }

   private List<String> processInputUrl(String url) {

      List<String> components = new ArrayList<>();

      if (!Pattern.matches("(https?://.*):(\\d*)/?(.*)", url)) {
         return components;
      }

      int posScheme = url.indexOf("/");
      int posPort = url.indexOf(":", posScheme);
      String scheme = url.substring(0, posScheme).replace(":", "");
      components.add(scheme);
      String host = url.substring(posScheme + 2, posPort);
      components.add(host);
      String port = url.substring(posPort + 1).replace("/", "");
      components.add(port);

      return components;

   }

   private RestExec setupClient(List<String> components) {

      ClientBuilder cb = new ClientBuilder();
      cb.setScheme(components.get(0));
      cb.setHost(components.get(1));
      cb.setPort(Integer.parseInt(components.get(2)));
      cb.setUser(components.get(3));
      cb.setPassword(components.get(4));

      boolean secure = true;

      if (StringUtils.isEmpty(components.get(3))) {
         secure = false;
      }


      HttpClient client = cb.build();

      return new RestExec()
         .setClient(client)
         .setHttpHost(cb.getHttpHost())
         .setSecured(secure);

   }

}
