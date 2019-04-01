package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.Constants;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class HostIdentifierCmd implements Command {

    /**
     * In this command the we look for the log location and pid. To do this we need
     * to identify which host the utility is running on. If it's more than a single node cluster
     * we use the input host hame if one was passed in. If it's a multi-node cluster and they used
     * localhost wee need to query the interfaces to see which one it was.
     */
    private final Logger logger = LogManager.getLogger(HostIdentifierCmd.class);

    public void execute(DiagnosticContext context) {

        logger.info("Checking the supplied hostname against the node information retrieved to verify location. This may take some time if localhost was specified in a multi-node cluster");
        String version = context.getVersion();
        String temp = context.getTempDir();
        String targetHost = context.getDiagnosticInputs().getHost();

        JsonNode rootNode = JsonYamlUtils.createJsonNodeFromFileName(temp, Constants.NODES);
        JsonNode nodes = rootNode.path("nodes");

        // First check for indications that there are docker containers running, in which case the
        // pid will be 1. And the IP may or may not match up.
        if(StringUtils.isNotEmpty(context.getDiagnosticInputs().getDockerId()) || dockerContainersPresent(nodes)){
            List<String> containers = getDockerContainerIds();
            if(containers.size() > 0){
                context.setDockerContainers(containers);
            }
            logger.info("Docker containers detected, bypassing host checks.");
            context.setDocker(true);
            return;
        }

        JsonNode targetNode;

        if (nodes.size() > 1) {
            Set<String> localAddr;
            // If they used a local address and there's more than one node we need to see what the IP addresses are
            if (Constants.LOCAL_ADDRESSES.contains(targetHost.toLowerCase())) {
                localAddr = SystemUtils.getNetworkInterfaces();
            }
            // Otherwise just use the host they passed in. It will have exactly one value.
            else {
                localAddr = new HashSet<>();
                localAddr.add(targetHost);
            }

            // Find the host the diagnostic was run against - if it's a true
            // production node it have a non-localhost address
            targetNode = findTargetNode(localAddr, version, nodes);
        } else {
            // If there's only one it's probably a dev node and not worth the effort
            targetNode = nodes.iterator().next();
        }

        if (targetNode == null) {
            logger.log(SystemProperties.DIAG, "{} host was not found in the nodes output", targetHost);
            context.setPid(Constants.NOT_FOUND);
            context.setLogDir(Constants.NOT_FOUND);

        } else {
            String pid = targetNode.path("process").path("id").asText();
            context.setPid(pid);
            String logDir = targetNode.path("settings").path("path").path("logs").asText();
            context.setLogDir(logDir);
        }

    }

    /**
     * @param hostsAndIps - String set containing either a single host name that was passed in, or if localhost was used and it is bigger than a one node cluster, a complete list of possible IP And host names.
     * @param version     - Elasticsearch version, since the place to find this could change.
     * @param nodes       - JsonNode containing the output of the _nodes command
     * @return - JsonNode running on the host the utility is being executed on or null if it couldn't be identified.
     */
    public JsonNode findTargetNode(Set<String> hostsAndIps, String version, JsonNode nodes) {

        for (String targetHost : hostsAndIps) {
            for (JsonNode node : nodes) {

                if (targetHost.equals(node.path("settings").path("network").path("host").asText())) {
                    return node;
                }

                if (targetHost.equals(node.path("host").asText())) {
                    return node;
                }

                if (targetHost.equals(node.path("ip").asText())) {
                    return node;
                }


                if (version.startsWith("1.")) {
                    String publishAddress = node.path("http").path("publish_address").asText();
                    int idxa = publishAddress.indexOf("/");
                    int idxb = publishAddress.indexOf(":");
                    publishAddress = publishAddress.substring(idxa, idxb - 1);
                    if (targetHost.equals(publishAddress)) {
                        return node;
                    }
                } else {
                    JsonNode bnds = node.path("http").path("bound_address");
                    if (bnds instanceof ArrayNode) {
                        ArrayNode boundAddresses = (ArrayNode) bnds;
                        for (JsonNode bnd : boundAddresses) {
                            String addr = bnd.asText();
                            addr = addr.substring(0, addr.indexOf(":"));
                            if (targetHost.equals(addr)) {
                                return node;
                            }
                        }
                    }
                }
            }
        }

        return null;

    }

    public boolean dockerContainersPresent(JsonNode nodes){
        for(JsonNode node : nodes){
            String processId = node.path("process").path("id").asText();
            String jvmPid = node.path("jvm").path("pid").asText();
            if("1".equals(processId) || "1".equals(jvmPid) ){
                return true;
            }
        }
        return false;

    }

    public List<String> getDockerContainerIds(){

        ProcessBuilder pb = new ProcessBuilder("docker", "ps", "-q");
        List<String> containers = new ArrayList();
        try {
            final Process p = pb.start();
            p.waitFor();
            InputStreamReader processReader = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(processReader);
            String line;
            while((line = br.readLine()) != null){
                containers.add(line);
            }
            
            p.destroy();

        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Error obtaining Docker Container Id's");

        }
        return containers;

    }


}
