package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class ExtractNodeInfoCmd extends AbstractDiagnosticCmd {

    public boolean execute(DiagnosticContext context){

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree("");
            JsonNode nodes = root.path("nodes");
            Iterator<JsonNode> it = nodes.iterator();

            Map<String, Object> cluster = new HashMap<>();
            List nodeList = new ArrayList();
            cluster.put("nodes", nodeList);

            while (it.hasNext()) {
                JsonNode n = it.next();

                String host = n.path("host").asText();
                String transportSocket = n.path("transport_address").asText();
                String[] transportAddress = transportSocket.split(":");
                String httpSocket = n.path("http_address").asText();
                String [] httpAddress = httpSocket.split(":");
                String name = n.path("name").asText();

                assert transportAddress.length == 2;
                assert httpAddress.length == 2;


               JsonNode settings = n.path("settings");
                String configFile = settings.path("config").asText();

                JsonNode nodePaths = settings.path("path");
                String logs = nodePaths.path("logs").asText();
                String conf = nodePaths.path("conf").asText();
                String home = nodePaths.path("home").asText();

                JsonNode jnode = n.path("process");
                String pid = jnode.path("id").asText();

                Map<String, String> tmp = new HashMap<>();
                tmp.put("host", host);
                tmp.put("transportAddress", transportAddress[0]);
                tmp.put("transportPort", transportAddress[1]);
                tmp.put("httpAddress", httpAddress[0]);
                tmp.put("httpPort", httpAddress[1]);

                tmp.put("name", name);
                tmp.put("config", configFile);
                tmp.put("conf", conf);
                tmp.put("logs", logs);
                tmp.put("home", home);
                tmp.put("pid", pid);
                nodeList.add(tmp);

                logger.debug("processed node:\n" + tmp);
            }

            String manifestString = mapper.writeValueAsString(cluster);
            context.setManifest(manifestString);
            context.setNodeString("");
        } catch (Exception e) {
            logger.error("Error creating the manifest file\n", e);
            context.addMessage("Could not generate the manifest. Some output will not be available.");
        }

        return true;
    }

}
