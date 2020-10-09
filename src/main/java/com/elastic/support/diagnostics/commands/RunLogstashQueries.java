package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.diagnostics.JavaPlatform;
import com.elastic.support.diagnostics.ProcessProfile;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
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
            RestEntryConfig builder = new RestEntryConfig("1.0.0");
            Map restCalls = JsonYamlUtils.readYamlFromClasspath(Constants.LS_REST, true);
            Map<String, RestEntry> entries = builder.buildEntryMap(restCalls);

            List<RestEntry> queries = new ArrayList<>();
            queries.addAll(entries.values());
            runQueries(ResourceCache.restClient, queries, context.tempDir, 0, 0, true);

            // Get the information we need to run system calls. It's easier to just get it off disk after all the REST calls run.
            ProcessProfile nodeProfile = new ProcessProfile();
            context.targetNode = nodeProfile;
            JsonNode nodeData = JsonYamlUtils.createJsonNodeFromFileName(context.tempDir, "logstash_node.json");
            nodeProfile.pid = nodeData.path("jvm").path("pid").asText();

            nodeProfile.os = SystemUtils.parseOperatingSystemName(nodeData.path("os").path("name").asText());
            nodeProfile.javaPlatform = new JavaPlatform(nodeProfile.os);
            if (StringUtils.isEmpty(nodeProfile.pid) || nodeProfile.pid.equals("1")) {
                context.dockerPresent = true;
                context.runSystemCalls = false;
            }
            // Create and cache the system command type we need, local or remote...
            SystemCommand syscmd = null;
            switch (context.diagnosticInputs.diagType) {
                case Constants.logstashRemote:
                    String targetOS;
                    if(context.dockerPresent){
                        targetOS = Constants.linuxPlatform;
                    }
                    else{
                        targetOS = nodeProfile.os;
                    }
                    ResourceCache.systemCommand = new RemoteSystem(
                            targetOS,
                            context.diagnosticInputs.remoteUser,
                            context.diagnosticInputs.remotePassword,
                            context.diagnosticInputs.host,
                            context.diagnosticInputs.remotePort,
                            context.diagnosticInputs.keyfile,
                            context.diagnosticInputs.pkiPass,
                            context.diagnosticInputs.knownHostsFile,
                            context.diagnosticInputs.trustRemote,
                            context.diagnosticInputs.isSudo
                    );
                    break;

                case Constants.logstashLocal:
                    if (context.dockerPresent) {
                        ResourceCache.systemCommand = new LocalSystem(SystemUtils.parseOperatingSystemName(SystemProperties.osName));
                    } else {
                        ResourceCache.systemCommand  = new LocalSystem(nodeProfile.os);
                    }
                    break;

                default:
                    // If it's not one of the above types it shouldn't be here but try to keep going...
                    context.runSystemCalls = false;
                    throw new RuntimeException("Host/Platform check error.");
            }


        } catch (Throwable t) {
            logger.error( "Logstash Query error:", t);
            throw new DiagnosticException(String.format("Error obtaining logstash output and/or process id - will bypass the rest of processing.. %s", Constants.CHECK_LOG));
        }
    }
}
