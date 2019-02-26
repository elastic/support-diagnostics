package com.elastic.support.diagnostics.chain;

import com.elastic.support.config.DiagnosticInputs;
import com.elastic.support.rest.RestExec;
import com.elastic.support.util.JsonYamlUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Map;

public class GlobalContext {

    private static Logger logger = LogManager.getLogger(GlobalContext.class);

    // These should all be immutable after initialization
    private static RestExec restExec;
    private static Map config;
    private static Map<String, Object> chains;
    private static boolean initialized = false;
    private static JsonNode nodeManifest;
    private static DiagnosticInputs diagnosticInputs;



    private static void init(DiagnosticInputs diagnosticInputs){

        GlobalContext.diagnosticInputs = diagnosticInputs;

        try {
            Map<String, Object> configuration = JsonYamlUtils.readYamlFromClasspath("diags.yml", true);
            if (configuration.size() == 0) {
                logger.error("Required config file diags.yml was not found. Exiting application.");
                throw new RuntimeException("Missing diags.yml");
            }

            GlobalContext.config = configuration;


            Map<String, Object> chains = JsonYamlUtils.readYamlFromClasspath("chains.yml", false);
            if (chains.size() == 0) {
                logger.error("Required config file chains.yml was not found. Exiting application.");
                throw new RuntimeException("Missing chain.yml");
            }

            GlobalContext.chains = chains;

            Map<String, Integer> restSettings = (Map<String, Integer>)configuration.get("rest-settings");

            GlobalContext.restExec = new RestExec(diagnosticInputs, restSettings);


        } catch (Exception e) {
            logger.error("Error encountered running diagnostic. See logs for additional information.  Exiting application.", e);
            throw new RuntimeException("DiagnosticService runtime error", e);
        }

    }


    public static  RestExec getRestExec() {
        if(initialized){
            return GlobalContext.restExec;
        }
        else{
            throw new IllegalStateException("Context not initialized");
        }

    }

    public static DiagnosticInputs getDiagnosticInputs() {
        if(initialized){
            return GlobalContext.diagnosticInputs;
        }
        else{
            throw new IllegalStateException("Context not initialized");
        }
    }

    public static Map getConfig() {
        if(initialized){
            return GlobalContext.config;
        }
        else{
            throw new IllegalStateException("Context not initialized");
        }
    }

    public static Map getChains(){
        if(initialized){
            return GlobalContext.chains;
        }
        else{
            throw new IllegalStateException("Context not initialized");
        }
    }

/*    public static JsonNode getNodeManifest() {
        return nodeManifest;
    }

    public static void setNodeManifest(JsonNode nodeManifest) {
        GlobalContext.nodeManifest = nodeManifest;
    }*/
}
