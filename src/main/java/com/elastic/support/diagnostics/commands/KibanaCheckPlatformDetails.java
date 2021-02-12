package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.JavaPlatform;
import com.elastic.support.diagnostics.ProcessProfile;
import com.elastic.support.diagnostics.commands.CheckPlatformDetails;
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

public class KibanaCheckPlatformDetails extends CheckPlatformDetails {

    /**
    * Check and collect information about the platform were Kibana is installed and store it in the context
    *
    * @param  DiagnosticContext context
    *
    * @return         List<ProcessProfile>
    */
    @Override
    public void execute(DiagnosticContext context) {

        try {
            // Cached from previous executions
            RestClient restClient = ResourceCache.getRestClient(Constants.restInputHost);

            // Populate the node metadata
            Map<String, RestEntry> calls = context.elasticRestCalls;
            RestEntry entry = calls.get("kibana_node_stats");
            RestResult result = restClient.execQuery(entry.getUrl());

            // Initialize to empty node which mimimizes NPE opportunities
            JsonNode infoNodes = JsonYamlUtils.mapper.createObjectNode();
            if (result.getStatus() == 200) {
                infoNodes = JsonYamlUtils.createJsonNodeFromString(result.toString());
            }

            context.clusterName = infoNodes.path("kibana").path("name").asText();

            List<ProcessProfile> nodeProfiles = getNodeNetworkAndLogInfo(infoNodes);

            // See if this cluster is dockerized - if so, don't bother checking for a master to go to
            // because the port information in its output is not reliable.
            for (ProcessProfile profile : nodeProfiles) {
                if (profile.isDocker) {
                    context.dockerPresent = true;
                    break;
                }
            }

            SystemCommand syscmd = null;

            switch (context.diagnosticInputs.diagType) {
                case Constants.kibanaRemote:
                    // If Docker containers flag it for no syscalls or logs
                    String targetOS;
                    if (context.dockerPresent) {
                        context.runSystemCalls = false;
                        // We really don't have any way of really knowing
                        // what the enclosing platform is. So this may fail...
                        logger.warn(Constants.CONSOLE, "Docker containers detected on remote platform - unable to determine host OS. Linux will be used but if another operating system is present the Docker diagnostic calls may fail.");
                        targetOS = Constants.linuxPlatform;
                        //break;
                    }
                    else{
                        context.targetNode = findTargetNode(
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

                case Constants.kibanaLocal:
                    if(context.dockerPresent){
                        context.runSystemCalls = false;

                        // We do need a system command local to run the docker calls
                        syscmd = new LocalSystem(SystemUtils.parseOperatingSystemName(SystemProperties.osName));
                        ResourceCache.addSystemCommand(Constants.systemCommands, syscmd);
                        break;
                    }

                    // Kibana is not working in cluster mode, so there is only one nodeprofile
                    if (nodeProfiles.size() == 1) {
                        context.targetNode = nodeProfiles.get(0);
                    }

                    context.targetNode = findTargetNode(
                            context.diagnosticInputs.host, nodeProfiles);

                    syscmd = new LocalSystem(context.targetNode.os);
                    ResourceCache.addSystemCommand(Constants.systemCommands, syscmd);

                    break;

                default:
                    // If it's not one of the above types it shouldn't be here but try to keep going...
                    context.runSystemCalls = false;
                    logger.warn(Constants.CONSOLE, "Error occurred checking the network hosts information. Bypassing system calls.");
                    throw new RuntimeException("Host/Platform check error.");

            }

        } catch (Exception e) {
            // Try to keep going even if this didn't work.
            logger.error(Constants.CONSOLE,"Error: {}", e.getMessage());
            logger.error( "Error checking node metadata and deployment info.", e);
            context.runSystemCalls = false;
        }
    }


    /**
    * Map Kibana / stats API results to the ProcessProfile object. Workaround to be able to test this function.
    *
    * @param  JsonNode nodesInfo
    *
    * @return         List<ProcessProfile>
    */
    public List<ProcessProfile> getNodeNetworkAndLogInfo(JsonNode nodesInfo) {

        List<ProcessProfile> nodeNetworkInfo = new ArrayList<>();
        try {
            ProcessProfile diagNode = new ProcessProfile();
            diagNode.name = nodesInfo.path("kibana").path("name").asText();
            diagNode.pid = nodesInfo.path("process").path("pid").asText();

            if (diagNode.pid.equals("1")) {
                diagNode.isDocker = true;
            }

            diagNode.networkHost = nodesInfo.path("kibana").path("host").asText();
            diagNode.host = nodesInfo.path("kibana").path("host").asText();

            String nodeOs = nodesInfo.path("os").path("platform").asText().toLowerCase();
            diagNode.os = SystemUtils.parseOperatingSystemName(nodeOs);
            diagNode.javaPlatform = new JavaPlatform(diagNode.os);

            String httpPublishAddr = nodesInfo.path("kibana").path("transport_address").asText();
            diagNode.httpPublishAddr = httpPublishAddr.substring(0, httpPublishAddr.indexOf(":"));
            diagNode.httpPort = Integer.parseInt(httpPublishAddr.substring(httpPublishAddr.indexOf(":") + 1));

            nodeNetworkInfo.add(diagNode);
        } catch (Exception e) {
            logger.error( "Error extracting node network addresses from nodes output", e);
        }

        return nodeNetworkInfo;
    }

    /**
    * Kibana is working in single mode, so the target node will be the only host stored on the nodeProfiles List.
    *
    * @param  String host
    * @param  List<ProcessProfile> nodeProfiles
    *
    * @return         String
    */
    public ProcessProfile findTargetNode(String host, List<ProcessProfile> nodeProfiles) {

        if (nodeProfiles.size() > 1) {
            logger.error("Kibana can not run in cluster mode, API response is not for the current process");
            throw new RuntimeException("Kibana can not run in cluster mode.");
        }
        return  nodeProfiles.get(0);
    }

}



