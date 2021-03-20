package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.JavaPlatform;
import com.elastic.support.diagnostics.ProcessProfile;
import com.elastic.support.diagnostics.commands.CheckPlatformDetails;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestEntry;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.ResourceCache;
import com.elastic.support.util.LocalSystem;
import com.elastic.support.util.RemoteSystem;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import com.elastic.support.util.SystemCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;


/**
 * {@code KibanaGetDetails} uses the network configuration defined in Kibana Stats API
 * to connect to the Local or Remote instance.
 *
 * If this request fails, then the rest of the diagnostic cannot proceed, as we need to 
 * have enough details to connect to Kibana instace.
 */
public class KibanaGetDetails extends CheckPlatformDetails {

    /**
    * Collect server details from Kibana using the configured {@code context}, and store
    * updated details back into the {@code context}.
    * @param  context
    */
    @Override
    public void execute(DiagnosticContext context) {

        try {
            final JsonNode infoProcess = getStats(context);

            context.clusterName = infoProcess.path("kibana").path("name").asText();

            List<ProcessProfile> profiles = getNodeNetworkAndLogInfo(infoProcess);

            IsRunningInDocker(context, profiles);

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
                        context.targetNode = findTargetNode(profiles);
                        targetOS = context.targetNode.os;
                    }

                    ResourceCache.addSystemCommand(Constants.systemCommands, new RemoteSystem(
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
                    ));

                    break;

                case Constants.kibanaLocal:
                    if(context.dockerPresent){
                        context.runSystemCalls = false;

                        // We do need a system command local to run the docker calls
                        SystemCommand syscmd = new LocalSystem(SystemUtils.parseOperatingSystemName(SystemProperties.osName));
                        ResourceCache.addSystemCommand(Constants.systemCommands, syscmd);
                        break;
                    }

                    // Kibana is not working in cluster mode, so there is only one nodeprofile
                    if (profiles.size() == 1) {
                        context.targetNode = profiles.get(0);
                    }

                    context.targetNode = findTargetNode(profiles);

                    SystemCommand syscmd = new LocalSystem(context.targetNode.os);
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
            logger.error("Error fetching Kibana server details.", e);
            context.runSystemCalls = false;
        }
    }


    /**
    * Map Kibana / stats API results to the ProcessProfile object.
    *
    * @param  processInfo all the context sent by the kibana stats API
    *
    * @return The network info as defined in the kibana stats API
    */
    public List<ProcessProfile> getNodeNetworkAndLogInfo(JsonNode processInfo) {

        List<ProcessProfile> nodeNetworkInfo = new ArrayList<>();
        try {
            ProcessProfile diagNode = new ProcessProfile();
            diagNode.name = processInfo.path("kibana").path("name").asText();
            diagNode.pid = processInfo.path("process").path("pid").asText();

            if (diagNode.pid.equals("1")) {
                diagNode.isDocker = true;
            }

            diagNode.networkHost = processInfo.path("kibana").path("host").asText();
            diagNode.host = processInfo.path("kibana").path("host").asText();

            String nodeOs = processInfo.path("os").path("platform").asText().toLowerCase();
            diagNode.os = SystemUtils.parseOperatingSystemName(nodeOs);
            diagNode.javaPlatform = new JavaPlatform(diagNode.os);

            String httpPublishAddr = processInfo.path("kibana").path("transport_address").asText();
            diagNode.httpPublishAddr = httpPublishAddr.substring(0, httpPublishAddr.indexOf(":"));
            diagNode.httpPort = Integer.parseInt(httpPublishAddr.substring(httpPublishAddr.indexOf(":") + 1));

            nodeNetworkInfo.add(diagNode);
        } catch (Exception e) {
            logger.error("Error extracting Kibana network addresses from stats output", e);
        }

        return nodeNetworkInfo;
    }

    /**
    * Get the Kibana server instance's profile.
    *
    * @param  host
    * @param  profiles list of network information for each kibana instance running
    *
    * @return return the profile for the kibana process
    * @throws RuntimeException if there is not exactly one profile.
    */
    private void IsRunningInDocker(DiagnosticContext context, List<ProcessProfile> profiles) {
        // See if this process is dockerized - if so, don't bother checking for a master to go to
        // because the port information in its output is not reliable.
        for (ProcessProfile profile : profiles) {
            if (profile.isDocker) {
                context.dockerPresent = true;
                return;
            }
        }
    }

    /**
    * Get the Kibana server instance's profile.
    *
    * @param  host
    * @param  profiles list of network information for each kibana instance running
    *
    * @return return the profile for the kibana process
    * @throws RuntimeException if there is not exactly one profile.
    */
    public ProcessProfile findTargetNode(List<ProcessProfile> profiles) {
        if (profiles.size() > 1) {
            logger.error("Expected [1] Kibana process profile, but found [{}]", profiles.size());
            throw new RuntimeException("Unable to get Kibana process profile.");
        }

        return  profiles.get(0);
    }

    /**
     * Fetch the Kibana Stats JSON payload from the Kibana server.
     *
     * @param context The current diagnostic context
     * @return Never {@code null}.
     * @throws DiagnosticContext if Kibana responds with a non-200 response
    */
    public JsonNode getStats(DiagnosticContext context) {

        RestClient restClient = ResourceCache.getRestClient(Constants.restInputHost);
        String url = context.elasticRestCalls.get("kibana_stats").getUrl();
        RestResult result = restClient.execQuery(url);

        if (result.getStatus() != 200) {
            throw new DiagnosticException(
                String.format(
                    "Kibana responded with [%d] for [%s]. Unable to proceed.",
                    result.getStatus(), url
                )
            );
        }

        return JsonYamlUtils.createJsonNodeFromString(result.toString());
    }

}
