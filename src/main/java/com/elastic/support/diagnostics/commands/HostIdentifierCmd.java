package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.Constants;
import com.elastic.support.diagnostics.DiagnosticException;
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

        String targetHost = context.getDiagnosticInputs().getHost();
        Set<String> localAddr = SystemUtils.getNetworkInterfaces();
        localAddr = excludeLocal(localAddr);

        List<NodeNetworking> nodeAddrInfo = getNodeNetworkAndLogInfo(context);

        // Since pids check for indications that there are docker containers running, in which case the
        // pid will be 1.
        if (StringUtils.isNotEmpty(context.getDiagnosticInputs().getDockerId()) || dockerContainersPresent(nodeAddrInfo)) {
            List<String> containers = getDockerContainerIds();
            if (containers.size() > 0) {
                context.setDockerContainers(containers);
            }
            logger.info("Docker containers detected, bypassing host checks.");
            context.setDocker(true);
            context.setBypassSystemCalls(true);;
            return;
        }

        NodeNetworking targetNode = null;
        try {
            targetNode = findTargetNode(localAddr, nodeAddrInfo, targetHost);
            context.setLogDir(targetNode.logDir);
        } catch (Exception e) {
            context.setBypassSystemCalls(true);
            logger.warn("Could not match target node or local address info to a node in the cluster. See archived logs for more details.");
            logger.warn("System calls and log collection will be disabled.");
        }

    }

    private Set<String> excludeLocal(Set<String> localAddr) {
        // A hit on localhost doesn't mean anything so just get rid of it.
        Set<String> newAddr = new HashSet<>();
        for (String addr : localAddr) {
            if (!Constants.LOCAL_ADDRESSES.contains(addr.toLowerCase())) {
                newAddr.add(addr);
            }
        }
        return newAddr;
    }


    private NodeNetworking findTargetNode(Set<String> localAddrs, List<NodeNetworking> nodeAddrs,
                                          String targetHost) {

        // If there's only one node and it's localhost, no big deal. Just use that node.
        if (Constants.LOCAL_ADDRESSES.contains(targetHost.toLowerCase()) && nodeAddrs.size() == 1) {
            return nodeAddrs.get(0);
        }

        // First check and see if we got a hit on the target host while we
        // got the nodes' bindings. If so just return that and get out.
        for (NodeNetworking nn : nodeAddrs) {
            if (nn.isTargetHost) {
                return nn;
            }
        }

        // Otherwise we need to identify by comparing the list of
        // local host/ip's to the ones in the nodes.
        // If we don't get those we won't be able to get logs or
        // run pid related system commands like the thread dump.
        // Go through each local address and see if it gets a hit in the node addresses
        // we extracted.
        for (String localAddr : localAddrs) {
            for (NodeNetworking nn : nodeAddrs) {
                // If this addresses bound to the nics on this host are contained on the current node
                // send back the data object containing the required info.
                if (nn.hostAndIp.contains(localAddr)) {
                    return nn;
                }
            }
        }

        // If we got this far and came up empty, signal our displeasure

        logger.log(SystemProperties.DIAG,"Comparison did not result in an IP or Host match. {} {}", localAddrs, nodeAddrs);
        throw new RuntimeException();
    }


    private List<NodeNetworking> getNodeNetworkAndLogInfo(DiagnosticContext context) {

        String temp = context.getTempDir();
        String targetHost = context.getDiagnosticInputs().getHost();
        List<NodeNetworking> nodeNetworkInfo = new ArrayList<>();

        try {
            JsonNode rootNode = JsonYamlUtils.createJsonNodeFromFileName(temp, Constants.NODES);
            JsonNode nodes = rootNode.path("nodes");
            NodeNetworking nn = new NodeNetworking();

            for (JsonNode node : nodes) {
                nn.pid = node.path("process").path("id").asText();
                nn.jvmPid = node.path("jvm").path("pid").asText();
                nn.name = node.path("name").asText();
                nn.logDir = node.path("settings").path("path").path("logs").asText();
                nn.hostAndIp.add(node.path("settings").path("network").path("host").asText());
                nn.hostAndIp.add(node.path("host").asText());
                nn.hostAndIp.add(node.path("ip").asText());

                // This won't work for version 1. Ask me if I care?
                JsonNode bnds = node.path("http").path("bound_address");
                if (bnds instanceof ArrayNode) {
                    ArrayNode boundAddresses = (ArrayNode) bnds;
                    for (JsonNode bnd : boundAddresses) {
                        String addr = bnd.asText();
                        addr = addr.substring(0, addr.indexOf(":"));
                        nn.hostAndIp.add(addr);

                    }
                }

                // If we see the targetHost in the interfaces and it's not localhost flag it.
                if (!Constants.LOCAL_ADDRESSES.contains(targetHost)) {
                    nn.isTargetHost = nn.hostAndIp.contains(targetHost);
                }
                nodeNetworkInfo.add(nn);
            }
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Error extracting node network addresses from nodes output", e);
        }

        return nodeNetworkInfo;
    }

    public boolean dockerContainersPresent(List<NodeNetworking> nodes) {

        // If there are pids of 1 in any of these the installation isn't
        // as cut and dries as they think.
        for(NodeNetworking node : nodes) {
            if ("1".equals(node.pid) || "1".equals(node.jvmPid)) {
                return true;
            }
        }

        return false;

    }

    public List<String> getDockerContainerIds() {

        ProcessBuilder pb = new ProcessBuilder("docker", "ps", "-q");
        List<String> containers = new ArrayList();
        try {
            final Process p = pb.start();
            p.waitFor();
            InputStreamReader processReader = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(processReader);
            String line;
            while ((line = br.readLine()) != null) {
                containers.add(line);
            }

            p.destroy();

        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Error obtaining Docker Container Id's");

        }
        return containers;

    }

    // Used as a structure to avoid all the Map churn.
    class NodeNetworking {
        boolean isTargetHost;
        String name;
        String pid;
        String jvmPid;
        String logDir;
        Set<String> hostAndIp = new HashSet<>();

    }


}