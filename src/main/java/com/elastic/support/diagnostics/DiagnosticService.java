package com.elastic.support.diagnostics;

import com.elastic.support.InputParams;
import com.elastic.support.SystemProperties;
import com.elastic.support.SystemUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipOutputStream;

public class DiagnosticService {

    private static final Logger logger = LoggerFactory.getLogger(DiagnosticService.class);

    private RestTemplate restTemplate;

    public void run(InputParams inputs) {

        logger.debug(inputs.toString());

        // Get the yaml config file, either default or passed in
        Map configMap = retrieveConfiguration();

        // Create an SSL enabled version - it will work for regular HTTP as well.
        // Note that it will function like a browser where you tell it to go ahead and trust an unknown CA
        int connectTimeout = (Integer)configMap.get("connectTimeout");
        int requestTimeout =  (Integer)configMap.get("requestTimeout");

        DiagnosticRequestFactory diagnosticRequestFactory = new DiagnosticRequestFactory(connectTimeout, requestTimeout);
        restTemplate = new RestTemplate(diagnosticRequestFactory.getSslReqFactory());
        HttpEntity<String> request = configureAuth(inputs);

        // Get the version number and cluster name fromt the JSON returned
        // by just submitting the host/port combo
        Map resultMap = this.getVersionData(inputs.getUrl(), request);
        String clusterName = (String) resultMap.get("cluster_name");
        Map versionMap = (Map) resultMap.get("version");
        String version = (String) versionMap.get("number");

        // use the appropriate combination of statements - overlay the current with the prior ones
        // if using an older version
        Map<String, String> statements = getConfiguredStatements(version, configMap);

        // Set up where we want to put the results - it may come in from the command line
        String outputDir = setOutputDir(inputs);
        System.out.println("Results will be written to: " + outputDir);
        String tempDir = outputDir + SystemProperties.fileSeparator + clusterName + "-diagnostics";

        // Create the temp directory - delete if first if it exists from a previous run
        try {
            SystemUtils.deleteDir(tempDir);
            Files.createDirectories(Paths.get(tempDir));
        } catch (IOException e) {
            logger.error("Temp dir could not be created", e);
            throw new RuntimeException("Could not create temp directory - see logs for details.");
        }

        logger.debug("Created temp directory: " + tempDir);
        System.out.println("Creating " + tempDir + " as temporary directory.\n");

        Set<Map.Entry<String, String>> entries = statements.entrySet();

        for(Map.Entry<String, String> entry: entries){
            logger.debug("Generating full diagnostic.");
            String queryName = entry.getKey();
            String query = entry.getValue();
            logger.debug(": now processing " + queryName + ", " + query);
            runDiagnosticQuery(configMap, inputs.getUrl(), queryName, query, tempDir, request);
        }

        logger.debug("Finished retrieving queries.");
        processOsCmds(configMap, tempDir, inputs);
        zipResults(tempDir);

        System.out.println("Finished archiving results and deleting temp directories");

    }

    public Map<String, String> getConfiguredStatements(String version, Map configMap) {

        // use the appropriate combination of statements - overlay the current with the prior ones
        // if using an older version
        Map<String, String> statements = (Map<String, String>) configMap.get("restQueries");

        return statements;

    }

    public Map getVersionData(String url, HttpEntity<String> request) {

        Map versionMap;

        try {
            String result = submitRequest(url, "", request);
            ObjectMapper mapper = new ObjectMapper();
            versionMap = mapper.readValue(result, LinkedHashMap.class);
        } catch (Exception e) {
            logger.error("Error getting version.", e);
            throw new RuntimeException("Error retrieving version data. " + e.getMessage());
        }

        return versionMap;
    }

    public void runDiagnosticQuery(Map configMap, String url, String key, String query, String target, HttpEntity<String> request) {

        List textFileExtensions = (List) configMap.get("textFileExtensions");
        String result;

        try {
            result = submitRequest(url, query, request);
            String ext;
            if (textFileExtensions.contains(key)) {
                ext = ".txt";
            } else {
                ext = ".json";
            }
            String filename = target + SystemProperties.fileSeparator + key + ext;

            Files.write(Paths.get(filename), result.getBytes());

            logger.debug("Done writing:" + filename);
            System.out.println("Statistic " + key + " was retrieved and saved to disk.");

            //If it's nodes then we add to the the collection file output
            if (key.equalsIgnoreCase("nodes")) {
                String manifestString = writeClusterManifest(result, target);
                processLogAndConfigFiles(manifestString, target);
            }

        } catch (IOException ioe) {
            // If something goes wrong write the detail stuff to the log and then rethrow a RuntimeException
            // that will be caught at the top level and will contain a more generic user message
            logger.error("Diagnostic for:" + key + "couldn't be written", ioe);
            throw new RuntimeException("Error writing file for statistic:" + key + ". There may be issues with the file system.  You may need to check for permissions or space issues.");
        } catch (Exception e) {
            // If they aren't Shield users this will generate an Exception so if it fails just continue and don't rethrow an Exception
            if (!"licenses".equalsIgnoreCase(key)) {
                logger.error("Error retrieving the following diagnostic:  " + key + " - this stat will not be included.", e);
            }
        }
    }

    public String submitRequest(String url, String query, HttpEntity<String> request) {

        String result;
        try {
            String submission = url + "/" + query;
            logger.debug("Submitting: " + url);
            ResponseEntity<String> response = restTemplate.exchange(submission, HttpMethod.GET, request, String.class);
            result = response.getBody();
            logger.debug(result);

        } catch (RestClientException e) {
            if(query.contains("license")){
                logger.error("could not retrive license file.");
                return "no licenses installed";
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

    private String writeClusterManifest(String nodeString, String target) {

        String manifestString = null;

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(nodeString);
            JsonNode nodes = root.path("nodes");
            String clusterName = root.path("cluster_name").textValue();

            Iterator<JsonNode> it = nodes.iterator();

            Map<String, Object> cluster = new HashMap<>();
            cluster.put("clusterName", clusterName);
            cluster.put("collectionDate", SystemUtils.getUtcDateString());
            List nodeList = new ArrayList();
            cluster.put("nodes", nodeList);

            while (it.hasNext()) {
                JsonNode n = it.next();
                String host = n.path("host").asText();
                String ip = n.path("ip").asText();
                String name = n.path("name").asText();

                JsonNode settings = n.path("settings");
                String configFile = settings.path("config").asText();

                JsonNode nodePaths = settings.path("path");
                String logs = nodePaths.path("logs").asText();
                String conf = nodePaths.path("conf").asText();
                String home = nodePaths.path("home").asText();

                Map<String, String> tmp = new HashMap<>();
                tmp.put("host", host);
                tmp.put("ip", ip);
                tmp.put("name", name);
                tmp.put("config", configFile);
                tmp.put("conf", conf);
                tmp.put("logs", logs);
                tmp.put("home", home);
                nodeList.add(tmp);

                logger.debug("processed node:\n" + tmp);
            }

            File manifest = new File(target + SystemProperties.fileSeparator + clusterName + "-manifest.json");
            mapper.writeValue(manifest, cluster);
            manifestString = mapper.writeValueAsString(cluster);

        } catch (Exception e) {
            logger.error("Error parsing or writing the collector file:\n", e);
        }

        return manifestString;
    }

    public String setOutputDir(InputParams inputs) {

        if ("cwd".equalsIgnoreCase(inputs.getOutputDir())) {
            return SystemProperties.userDir;
        } else {
            return inputs.getOutputDir();
        }
    }


    public HttpEntity<String> configureAuth(InputParams inputs) {

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

    }
    public void zipResults(String dir) {

        try {
            File file = new File(dir);
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(dir + ".zip"));
            out.setLevel(ZipOutputStream.DEFLATED);
            SystemUtils.zipDir("", file, out);
            logger.debug("Archive " + dir + ".zip was created");
            file.delete();
            logger.debug("Temp directory " + dir + " was deleted.");
            out.close();

        } catch (Exception ioe) {
            logger.error("Couldn't create archive.\n", ioe);
            throw new RuntimeException(("Error creating compressed archive from statistics files." ));
        }
    }
    public Set getIpAndHostData(){

        // Check system for NIC's to get ip's and hostnames
        HashSet ipAndHosts = new HashSet();

        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();

            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                ipAndHosts.add(nic.getDisplayName());
                Enumeration<InetAddress> inets = nic.getInetAddresses();

                while (inets.hasMoreElements()) {
                    InetAddress inet = inets.nextElement();
                    ipAndHosts.add(inet.getHostAddress());
                    ipAndHosts.add(inet.getHostAddress());
                }
            }
        }
        catch (Exception e) {
            logger.error("Error occurred acquiring IP's and hostnames", e);
        }

        logger.debug("IP and Hostname list:" + ipAndHosts);
        return ipAndHosts;
    }

    public boolean processLogAndConfigFiles(String manifestString, String target) {

        boolean processed = false;
        try {
            Set ipAndHosts = this.getIpAndHostData();
            JsonNode rootNode = new ObjectMapper().readTree(manifestString);
            JsonNode nodes = rootNode.path("nodes");
            String clusterName = rootNode.path("clusterName").textValue();

            Iterator<JsonNode> it = nodes.iterator();

            while (it.hasNext()) {
                JsonNode n = it.next();
                String hostName = n.path("host").asText();
                String ip = n.path("ip").asText();

                // if the host we're on doesn't match up with the node entry
                // then bypass it and move to the next node
                if (! (ipAndHosts.contains(ip) || ipAndHosts.contains(hostName) ) ) {
                    continue;
                }

                String name = n.path("name").asText();
                String config = n.path("config").asText();
                String conf = n.path("conf").asText();
                String logs = n.path("logs").asText();
                String home = n.path("home").asText();

                // Create a directory for this node
                String nodeDir = target + SystemProperties.fileSeparator + name;
                Files.createDirectories(Paths.get(nodeDir));

                String configFileLoc = determineConfigLocation(conf, config, home);

                // Copy the config file
                SystemUtils.copyFile(configFileLoc, nodeDir + SystemProperties.fileSeparator + "elasticsearch.yml");

                if ("".equals(logs)) {
                    logs = home + SystemProperties.fileSeparator + "logs";
                }

                // Copy the main and slow logs
                SystemUtils.copyFile(logs + SystemProperties.fileSeparator + clusterName + ".log", nodeDir + SystemProperties.fileSeparator + clusterName + ".log");
                SystemUtils.copyFile(logs + SystemProperties.fileSeparator + clusterName + "_index_indexing_slowlog.log", nodeDir + SystemProperties.fileSeparator + clusterName + "_index_indexing_slowlog.log");
                SystemUtils.copyFile(logs + SystemProperties.fileSeparator + clusterName + "_index_search_slowlog.log", nodeDir + SystemProperties.fileSeparator + clusterName + "_index_search_slowlog.log");

                logger.debug("processed node:\n" + name);
                processed = true;
            }
        } catch (Exception e) {
            logger.error("Error processing the nodes manifest:\n", e);
            throw new RuntimeException("Error processing node");
        }

        return processed;

    }

    public String determineConfigLocation(String conf, String config, String home){

        String configFileLoc;

        //Check for the config location
        if (!"".equals(config)) {
            configFileLoc = config;
        } else if (!"".equals(conf)) {
            configFileLoc = conf + "elasticsearch.yml";
        } else {
            configFileLoc = home + SystemProperties.fileSeparator + "config" + SystemProperties.fileSeparator + "elasticsearch.yml";
        }

        return configFileLoc;
    }

    public String checkOS() {
        String osName = SystemProperties.osName.toLowerCase();
        if (osName.contains("windows")) {
            return "winOS";
        } else if (osName.contains("linux")) {
            return "linuxOS";
        } else if (osName.contains("darwin") || osName.contains("mac os x")) {
            return "macOS";
        }
        else {
            logger.error("Failed to detect operating system!");
            throw new RuntimeException("Unsupported OS");
        }
    }

    public void processOsCmds(Map configMap, String targetDir, InputParams inputs) {
        String os = checkOS();
        Map<String, String> osCmds = (Map<String, String>) configMap.get(os);

        ProcessBuilder pb = new ProcessBuilder();
        pb.redirectErrorStream(true);

        Iterator<Map.Entry<String, String>> iter = osCmds.entrySet().iterator();
        List cmds = new ArrayList();

        try {
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>) iter.next();
                String cmdLabel = entry.getKey();
                String cmdText = entry.getValue();
                StringTokenizer st = new StringTokenizer(cmdText, " ");
                while (st.hasMoreTokens()) {
                    cmds.add(st.nextToken());
                }

                pb.redirectOutput(new File(targetDir + SystemProperties.fileSeparator + cmdLabel + ".txt"));
                pb.command(cmds);
                Process pr = pb.start();
                pr.waitFor();
                cmds.clear();

            }
        } catch (Exception e) {
            logger.error("Error processing system commands", e);
        }
    }

    public Map retrieveConfiguration() {

        InputStream is;
        try {
            is = this.getClass().getClassLoader().getResourceAsStream("diags.yml");
            return SystemUtils.readYaml(is, true);

        } catch (Exception e) {
            logger.error("Error retriving configuration", e);
            throw new RuntimeException("Could not retrieve configuration - was a valid absolute path specified?");
        }
    }

    public void zipResults(String dir, String hostName) {

        try {
            File file = new File(dir);
            String zipFileName = dir + "-" + hostName + ".zip";
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
            out.setLevel(ZipOutputStream.DEFLATED);
            SystemUtils.zipDir("", file, out);
            logger.debug("Archive " + zipFileName);
            file.delete();
            logger.debug("Temp directory " + dir + " was deleted.");
            out.close();

        } catch (Exception ioe) {
            logger.error("Couldn't create archive.\n", ioe);
            throw new RuntimeException(("Error creating compressed archive from statistics files." ));
        }
    }


}


