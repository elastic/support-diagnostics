/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.util.JsonYamlUtils;
import co.elastic.support.util.LocalSystem;
import co.elastic.support.util.RemoteSystem;
import co.elastic.support.util.SystemCommand;
import co.elastic.support.util.SystemProperties;
import co.elastic.support.Constants;
import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.diagnostics.JavaPlatform;
import co.elastic.support.diagnostics.ProcessProfile;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.rest.RestClient;
import co.elastic.support.rest.RestEntry;
import co.elastic.support.rest.RestEntryConfig;
import co.elastic.support.util.SystemUtils;
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

    public void execute(DiagnosticContext context) throws DiagnosticException {

        try {
            RestClient client = context.resourceCache.getRestClient(Constants.restInputHost);
            List<RestEntry> entries = new ArrayList<>(context.elasticRestCalls.values());
            runQueries(client, entries, context.tempDir, 0, 0);

            // Get the information we need to run system calls. It's easier to just get it
            // off disk after all the REST calls run.
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
                    if (context.dockerPresent) {
                        targetOS = Constants.linuxPlatform;
                    } else {
                        targetOS = nodeProfile.os;
                    }
                    syscmd = new RemoteSystem(
                            targetOS,
                            context.diagnosticInputs.remoteUser,
                            context.diagnosticInputs.remotePassword,
                            context.diagnosticInputs.host,
                            context.diagnosticInputs.remotePort,
                            context.diagnosticInputs.keyfile,
                            context.diagnosticInputs.pkiKeystorePass,
                            context.diagnosticInputs.knownHostsFile,
                            context.diagnosticInputs.trustRemote,
                            context.diagnosticInputs.isSudo);
                    context.resourceCache.addSystemCommand(Constants.systemCommands, syscmd);
                    break;

                case Constants.logstashLocal:
                    if (context.dockerPresent) {
                        syscmd = new LocalSystem(SystemUtils.parseOperatingSystemName(SystemProperties.osName));
                    } else {
                        syscmd = new LocalSystem(nodeProfile.os);
                    }
                    context.resourceCache.addSystemCommand(Constants.systemCommands, syscmd);

                    break;

                default:
                    // If it's not one of the above types it shouldn't be here but try to keep
                    // going...
                    context.runSystemCalls = false;
            }

        } catch (Throwable t) {
            logger.error("Logstash Query error:", t);
            throw new DiagnosticException(String.format(
                    "Error obtaining logstash output and/or process id - will bypass the rest of processing.. %s",
                    Constants.CHECK_LOG));
        }
    }
}
