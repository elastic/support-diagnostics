package com.elastic.support.util;

import com.elastic.support.rest.RestClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;


public class ResourceCache{

    private static final Logger logger = LogManager.getLogger(ResourceCache.class);
    private static ConcurrentHashMap<String, Closeable> resources = new ConcurrentHashMap();

    public static boolean resourceExists(String name){
        return resources.containsKey(name);
    }

    public static void addSystemCommand(String name, SystemCommand systemCommand){
        // Log the error if they tried to overlay with a dupe but don't throw an exception.
        resources.putIfAbsent(name, systemCommand );
    }

    public static SystemCommand getSystemCommand(String name){
        if(resources.containsKey(name)){
            return (SystemCommand)resources.get(name);
        }

        throw new IllegalStateException("SystemCommand instance requested does not exist");
    }

    public static void addRestClient(String name, RestClient client){
        resources.putIfAbsent(name, client);
    }

    public static RestClient getRestClient(String name){
        if (resources.containsKey(name)){
            return (RestClient)resources.get(name);
        }
        throw new IllegalStateException("RestClient instance does not exist");
    }

    // Centralized method for cleaning up ssh and http clients
    public static void closeAll() {
        resources.forEach((name, resource)->{
                try{
                    resource.close();
                }
                catch (Exception e){
                    logger.log(SystemProperties.DIAG, "Failed to close resource {}", name);
                }
        });
    }
}
