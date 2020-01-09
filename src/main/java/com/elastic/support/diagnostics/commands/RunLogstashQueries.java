package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.diagnostics.ProcessProfile;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestEntry;
import com.elastic.support.rest.RestEntryConfig;
import com.elastic.support.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RunLogstashQueries extends BaseQuery {

    /**
     * Executes the REST calls for Logstash
     */

    private static final Logger logger = LogManager.getLogger(BaseQuery.class);

    public void execute(DiagnosticContext context) {

        try {
            RestClient client = ResourceCache.getRestClient(Constants.restInputHost);

            RestEntryConfig builder = new RestEntryConfig("1.0.0");
            Map restCalls = JsonYamlUtils.readYamlFromClasspath(Constants.LS_REST, true);
            Map<String, RestEntry> entries = builder.buildEntryMap(restCalls);

            List<RestEntry> queries = new ArrayList<>();
            queries.addAll(entries.values());
            runQueries(client, queries, context.tempDir, 0, 0);

            // Get the information we need to run system calls. It's easier to just get it off disk after all the REST calls run.
            ProcessProfile nodeProfile = new ProcessProfile();
            context.targetNode = nodeProfile;

            JsonNode nodeData = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "logstash_node.json");
            nodeProfile.pid = nodeData.path("jvm").path("pid").asText();
            ;
            nodeProfile.os = SystemUtils.parseOperatingSystemName(nodeData.path("os").path("name").asText());

            if (StringUtils.isEmpty(nodeProfile.pid) || nodeProfile.pid.equals("1")) {
                context.dockerPresent = true;
                context.runSystemCalls = false;
               // We do need a system command local to run the docker calls
               SystemCommand syscmd = new LocalSystem(SystemUtils.parseOperatingSystemName(SystemProperties.osName));
               ResourceCache.addSystemCommand(Constants.systemCommands, syscmd);
            } else {
                // Create and cache the system command type we need, local or remote...
                SystemCommand syscmd = null;
                switch (context.diagnosticInputs.diagType) {
                    case Constants.logstashRemote:

                        syscmd = new RemoteSystem(
                                context.targetNode.os,
                                context.diagnosticInputs.remoteUser,
                                context.diagnosticInputs.remotePassword,
                                context.diagnosticInputs.host,
                                context.diagnosticInputs.remotePort,
                                context.diagnosticInputs.keyfile,
                                context.diagnosticInputs.pkiKeystorePass,
                                context.diagnosticInputs.knownHostsFile,
                                context.diagnosticInputs.trustRemote,
                                context.diagnosticInputs.sudo
                        );
                        ResourceCache.addSystemCommand(Constants.systemCommands, syscmd);
                        break;

                    case Constants.logstashLocal:
                        syscmd = new LocalSystem(nodeProfile.os);
                        ResourceCache.addSystemCommand(Constants.systemCommands, syscmd);

                        break;

                    default:
                        // If it's not one of the above types it shouldn't be here but try to keep going...
                        context.runSystemCalls = false;
                        throw new RuntimeException("Host/Platform check error.");
                }
            }


        } catch (Throwable t) {
            logger.log(SystemProperties.DIAG, "Logstash Query error:", t);
            throw new DiagnosticException(String.format("Error obtaining logstash output and/or process id - will bypass the rest of processing.. %s", Constants.CHECK_LOG));
        }
    }


}
