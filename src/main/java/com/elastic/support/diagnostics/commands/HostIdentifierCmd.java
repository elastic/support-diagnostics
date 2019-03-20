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

import java.util.*;


public class HostIdentifierCmd implements Command {

    /**
     * In this command the list of network interfaces is obtained from the Java runtime
     * and compared against the addresses in the nodes output to determine which node
     * the diagnostic is running on. Note that this can be fairly slow on some OS's
     * particularly OSX on Macbooks.
     */
    private final Logger logger = LogManager.getLogger(HostIdentifierCmd.class);

    public void execute(DiagnosticContext context) {

        try {
            logger.info("Checking the supplied hostname against the node information retrieved to verify location. This may take some time...");
            String version = context.getVersion();
            String temp = context.getTempDir();
            String targetHost = context.getDiagnosticInputs().getHost();

            JsonNode rootNode = JsonYamlUtils.createJsonNodeFromFileName(temp, Constants.NODES);
            JsonNode nodes = rootNode.path("nodes");
            JsonNode targetNode;
            if(nodes.size() < 1){
                // Find the host the diagnostic was run against - if it's a true
                // production node it have a non-localhost address
                targetNode = findTargetNode(targetHost, version, nodes);
            }
            else{
                // If there's only one it's probably a dev node and not worth the effort
                targetNode = nodes.get(0);
            }

            Set<String> localAddr = SystemUtils.getNetworkInterfaces();

            if (targetNode == null) {
                logger.log(SystemProperties.DIAG, "{} host was not found in the nodes output", targetHost);

            } else {
                String pid = targetNode.path("process").path("id").asText();
                context.setPid(pid);
                String logDir = targetNode.path("settings").path("path").path("logs").asText();
                context.setLogDir(logDir);
            }

        } catch (Exception e) {
            context.setPid(Constants.NOT_FOUND);
            context.setLogDir(Constants.NOT_FOUND);
            logger.log(SystemProperties.DIAG, "Error identifying host of diag node.", e);
        }

    }

    private void addToHostList(String host, Set hostList) {
        if (!StringUtils.isEmpty(host)) {
            hostList.add(host);
        }
    }

    public JsonNode findTargetNode( String targetHost, String version, JsonNode nodes){

        for (JsonNode node : nodes) {

            if(targetHost.equals(node.path("settings").path("network").path("host").asText())){
                return node;
            }

            if(targetHost.equals(node.path("host").asText())){
                return node;
            }

            if(targetHost.equals(node.path("ip").asText())){
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
                        if(targetHost.equals(addr)){
                            return node;
                        }
                    }
                }
            }
        }

        return null;

    }

    private boolean isLocalNode(Set<String> localAddr, Set<String> nodeAddr) {

        for (String addr : localAddr) {
            if (nodeAddr.contains(addr)) {
                return true;
            }
        }

        return false;
    }

    private class NodeHost{
        JsonNode node;
        String host;
        public NodeHost(JsonNode node, String host){

        }

    }





}
