/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.util;

import co.elastic.support.rest.RestClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;


public class ResourceCache implements AutoCloseable {

    private final Logger logger = LogManager.getLogger(ResourceCache.class);
    private ConcurrentHashMap<String, Closeable> resources = new ConcurrentHashMap();

    public void addSystemCommand(String name, SystemCommand systemCommand){
        // Log the error if they tried to overlay with a dupe but don't throw an exception.
        resources.putIfAbsent(name, systemCommand );
    }

    public SystemCommand getSystemCommand(String name){
        if(resources.containsKey(name)){
            return (SystemCommand)resources.get(name);
        }

        throw new IllegalStateException("SystemCommand instance requested does not exist");
    }

    public void addRestClient(String name, RestClient client){
        resources.putIfAbsent(name, client);
    }

    public RestClient getRestClient(String name){
        if (resources.containsKey(name)){
            return (RestClient)resources.get(name);
        }
        throw new IllegalStateException("RestClient instance does not exist");
    }

    @Override
    public void close() {
        resources.forEach((name, resource)->{
                try{
                    resource.close();
                }
                catch (Exception e){
                    logger.error( "Failed to close resource {}", name);
                }
        });
    }
}
