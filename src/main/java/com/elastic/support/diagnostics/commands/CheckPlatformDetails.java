package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.JavaPlatform;
import com.elastic.support.diagnostics.ProcessProfile;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestEntry;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class CheckPlatformDetails implements Command {

    public static final String LOCAL_ADDRESSES = "127.0.0.1;localhost;::1";
    Logger logger = LogManager.getLogger(CheckPlatformDetails.class);

    @Override
    public void execute(DiagnosticContext context) {

        try {
            // Cached from previous executions
            RestClient restClient = ResourceCache.getRestClient(Constants.restInputHost);

            // Populate the node metadata
            Map<String, RestEntry> calls = context.elasticRestCalls;
            RestEntry entry = calls.get("nodes");
            String url = entry.getUrl().replace("?pretty", "/os,process,settings,transport,http?pretty&human");
            RestResult result = restClient.execQuery(url);

            // Initialize to empty node which mimimizes NPE opportunities
            JsonNode infoNodes = JsonYamlUtils.mapper.createObjectNode();
            if (result.getStatus() == 200) {
                infoNodes = JsonYamlUtils.createJsonNodeFromString(result.toString());
            }

            context.clusterName = infoNodes.path("cluster_name").asText();

            List<ProcessProfile> nodeProfiles = getNodeNetworkAndLogInfo(infoNodes);

            // See if this cluster is dockerized - if so, don't bother checking for a master to go to
            // because the port information in its output is not reliable.
            for (ProcessProfile profile : nodeProfiles) {
                if (profile.isDocker) {
                    context.dockerPresent = true;
                    break;
                }
            }

            // Removed temporarily until I put in an option to bypass this feature due to
            // issue where master was inaccessible via http from another node even though it had http configured.
/*            if (!context.dockerPresent) {
                // Get the master node id and flag the master node profile
                entry = calls.get("master");
                result = restClient.execQuery(entry.getUrl());
                JsonNode currentMaster = JsonYamlUtils.mapper.createObjectNode();
                if (result.getStatus() == 200) {
                    // Strip off the array brackets since there's only one at each end
                    String mod = result.toString();
                    mod = mod.substring(1, (mod.length() - 1));
                    currentMaster = JsonYamlUtils.createJsonNodeFromString(mod);
                }
                String currentMasterId = currentMaster.path("id").asText();
                ProcessProfile masterNode = findMasterNode(currentMasterId, nodeProfiles);

                // If the master node has an http listener configured then we'll use that for the
                // REST calls. Set up a rest client and add it to the resources.
                if (differentInstances(context.diagnosticInputs.host, context.diagnosticInputs.port, masterNode.host, masterNode.httpPort) && masterNode.isHttp) {
                    RestClient masterRestClient = RestClient.getClient(
                            masterNode.host,
                            masterNode.httpPort,
                            context.diagnosticInputs.scheme,
                            context.diagnosticInputs.user,
                            context.diagnosticInputs.password,
                            context.diagnosticInputs.proxyHost,
                            context.diagnosticInputs.proxyPort,
                            context.diagnosticInputs.proxyUser,
                            context.diagnosticInputs.proxyPassword,
                            context.diagnosticInputs.pkiKeystore,
                            context.diagnosticInputs.pkiKeystorePass,
                            context.diagnosticInputs.skipVerification,
                            context.diagsConfig.connectionTimeout,
                            context.diagsConfig.connectionRequestTimeout,
                            context.diagsConfig.socketTimeout);

                    ResourceCache.addRestClient(Constants.restTargetHost, masterRestClient);
                }
            }*/

            SystemCommand syscmd = null;
            switch (context.diagnosticInputs.diagType) {
                case Constants.remote:
                    // If Docker containers flag it for no syscalls or logs
                    String targetOS;
                    if (context.dockerPresent) {
                        context.runSystemCalls = false;
                        // We really don't have any way of really knowing
                        // what the enclosing platform is. So this may fail...
                        logger.info("Docker containers detected on remote platform - unable to determine host OS. Linux will be used but if another operating system is present the Docker diagnostic calls may fail.");
                        targetOS = Constants.linuxPlatform;
                        //break;
                    }
                    else{
                        // check for input host in bound addresses
                        context.targetNode = findRemoteTargetNode(
                                context.diagnosticInputs.host, nodeProfiles);
                        targetOS = context.targetNode.os;
                    }

                    syscmd = new RemoteSystem(
                            targetOS,
                            context.diagnosticInputs.remoteUser,
                            context.diagnosticInputs.remotePassword,
                            context.diagnosticInputs.host,
                            context.diagnosticInputs.remotePort,
                            context.diagnosticInputs.keyfile,
                            context.diagnosticInputs.keyfilePassword,
                            context.diagnosticInputs.knownHostsFile,
                            context.diagnosticInputs.trustRemote,
                            context.diagnosticInputs.isSudo
                    );
                    ResourceCache.addSystemCommand(Constants.systemCommands, syscmd);

                    break;

                case Constants.local:
                    if(context.dockerPresent){
                        context.runSystemCalls = false;

                        // We do need a system command local to run the docker calls
                        syscmd = new LocalSystem(SystemUtils.parseOperatingSystemName(SystemProperties.osName));
                        ResourceCache.addSystemCommand(Constants.systemCommands, syscmd);
                        break;
                    }

                    // Find out which node we're going to obtain logs and system commands from.
                    // First check: if it's a one node cluster, we're done.
                    if (nodeProfiles.size() == 1) {
                        context.targetNode = nodeProfiles.get(0);
                    }

                    // check against the local network interfaces
                    context.targetNode = findLocalTargetNode(
                            context.diagnosticInputs.host, nodeProfiles);

                    syscmd = new LocalSystem(context.targetNode.os);
                    ResourceCache.addSystemCommand(Constants.systemCommands, syscmd);

                    break;

                default:
                    // If it's not one of the above types it shouldn't be here but try to keep going...
                    context.runSystemCalls = false;
                    logger.info("Error occurred checking the network hosts information. Bypassing system calls.");
                    throw new RuntimeException("Host/Platform check error.");

            }

        } catch (Exception e) {
            // Try to keep going even if this didn't work.
            logger.info("Error: {}", e.getMessage());
            logger.log(SystemProperties.DIAG, "Error checking node metadata and deployment info.", e);
            context.runSystemCalls = false;
        }
    }

    private ProcessProfile findMasterNode(String masterId, List<ProcessProfile> nodeProfiles) {

        // Save off the node profile for the current master
        ProcessProfile targetNode = nodeProfiles.stream()
                .filter(node -> (masterId.equals(node.id)))
                .findAny()
                .orElse(null);

        if (targetNode == null) {
            logger.info("Could not find current master in node list.");
            throw new RuntimeException();
        }

        targetNode.currentMaster = true;
        return targetNode;
    }

    private List<ProcessProfile> getNodeNetworkAndLogInfo(JsonNode nodesInfo) {

        List<ProcessProfile> nodeNetworkInfo = new ArrayList<>();

        try {
            JsonNode nodes = nodesInfo.path("nodes");
            Iterator<Map.Entry<String, JsonNode>> iterNode = nodes.fields();

            while (iterNode.hasNext()) {
                Map.Entry<String, JsonNode> n = iterNode.next();
                String key = n.getKey();
                JsonNode node = n.getValue();
                ProcessProfile diagNode = new ProcessProfile();

                diagNode.id = key;
                diagNode.name = node.path("name").asText();
                diagNode.pid = node.path("process").path("id").asText();
                diagNode.jvmPid = node.path("jvm").path("pid").asText();

                if (diagNode.pid.equals("1") || diagNode.jvmPid.equals("1")) {
                    diagNode.isDocker = true;
                }

                JsonNode settings = node.path("settings");
                JsonNode path = settings.path("path");
                JsonNode logs = path.path("logs");

                diagNode.logDir = node.path("settings").path("path").path("logs").asText();
                diagNode.networkHost = node.path("settings").path("network").path("host").asText();
                diagNode.host = (node.path("host").asText());
                diagNode.ip = (node.path("ip").asText());
                String nodeOs = node.path("os").path("name").asText().toLowerCase();
                diagNode.os = SystemUtils.parseOperatingSystemName(nodeOs);
                diagNode.javaPlatform = new JavaPlatform(diagNode.os);

                String httpPublishAddr = node.path("http").path("publish_address").asText();
                diagNode.httpPublishAddr = httpPublishAddr.substring(0, httpPublishAddr.indexOf(":"));
                diagNode.httpPort = Integer.parseInt(httpPublishAddr.substring(httpPublishAddr.indexOf(":") + 1));

                // This won't work for version 1 - it will always be empty so it will
                // never go after logs.
                JsonNode bnds = node.path("http").path("bound_address");
                if (bnds instanceof ArrayNode) {
                    diagNode.isHttp = true;
                    ArrayNode boundAddresses = (ArrayNode) bnds;
                    for (JsonNode bnd : boundAddresses) {
                        String addr = bnd.asText();
                        // See if the bound address is a loopback. If so, we don't need it
                        boolean notLoopBack = true;
                        for(String loopback: Constants.localAddressList){
                            if(addr.contains(loopback)){
                                notLoopBack = false;
                            }
                        }
                        if (notLoopBack) {
                            addr = addr.substring(0, addr.indexOf(":"));
                            diagNode.boundAddresses.add(addr);
                        }
                    }
                }

                nodeNetworkInfo.add(diagNode);
            }
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Error extracting node network addresses from nodes output", e);
        }

        return nodeNetworkInfo;
    }

    public ProcessProfile findRemoteTargetNode(String host, List<ProcessProfile> nodeProfiles) {

        ProcessProfile targetNode = nodeProfiles.stream()
                .filter(node -> (host.equals(node.httpPublishAddr)))
                .findAny()
                .orElse(null);

        if (targetNode == null) {
            logger.info("Could not match node publish address to specified host. Bypassing system calls");
            throw new RuntimeException();
        }

        return targetNode;

    }

    public ProcessProfile findLocalTargetNode(String inputHost, List<ProcessProfile> nodeProfiles) {

        logger.info("Checking the supplied hostname against the node information retrieved to verify location. This may take some time.");

        // If the input host was a loopback we need to compare each of the none loopback addresses present
        // on this host to the set of bound http addresses in each node. If if a non-loopback was input
        // we can get away with just comparing that to the bound address list.
        Set<String> localNetworkInterfaces = excludeLoopback(SystemUtils.getNetworkInterfaces());;
        if (! Constants.localAddressList.contains(inputHost)) {
            localNetworkInterfaces.add(inputHost);
        }

        return findTargetNode(localNetworkInterfaces, nodeProfiles);

    }

    private Set<String> excludeLoopback(Set<String> localAddr) {
        // A hit on localhost doesn't mean anything so just get rid of it.
        Set<String> newAddr = new HashSet<>();
        for (String addr : localAddr) {
            if (!Constants.localAddressList.contains(addr.toLowerCase())) {
                newAddr.add(addr);
            }
        }
        return newAddr;
    }

    private ProcessProfile findTargetNode(Set<String> localAddrs, List<ProcessProfile> nodeAddrs) {

        // We need to identify by comparing the list of
        // local host/ip's to the ones in the nodes.
        // If we don't get those we won't be able to get logs or
        // run pid related system commands like the thread dump.
        // Go through each local address and see if it gets a hit in the node addresses
        // we extracted.
        for (String localAddr : localAddrs) {
            for (ProcessProfile node : nodeAddrs) {
                // If this addresses bound to the nics on this host are contained on the current node
                // send back the data object containing the required info.
                if (node.boundAddresses.contains(localAddr)) {
                    return node;
                }
            }
        }

        // If we got this far and came up empty, signal our displeasure
        logger.log(SystemProperties.DIAG, "Comparison did not result in an IP or Host match. {} {}", localAddrs, nodeAddrs);
        throw new RuntimeException("Could not find the target node.");
    }

    private boolean differentInstances(String host, int port, String masterHost, int masterPort) {
        if (host.equals(masterHost) && port == masterPort) {
            return false;
        }
        return true;

    }

}



