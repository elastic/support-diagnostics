/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.util;

import co.elastic.support.rest.RestClient;
import jline.console.ConsoleReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.beryx.textio.jline.JLineTextTerminal;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;


public class ResourceCache{

    private static final Logger logger = LogManager.getLogger(ResourceCache.class);
    private static ConcurrentHashMap<String, Closeable> resources = new ConcurrentHashMap();

    public static final TextIO textIO = TextIoFactory.getTextIO();
    public static final TextTerminal terminal = textIO.getTextTerminal();

    static {
        if(terminal instanceof JLineTextTerminal){
            JLineTextTerminal jltt = (JLineTextTerminal)terminal;
            ConsoleReader reader = jltt.getReader();
            reader.setExpandEvents(false);
        }
    }

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

    // Centralized method for cleaning up console, ssh and http clients
    public static void closeAll() {
        resources.forEach((name, resource)->{
                try{
                    resource.close();
                }
                catch (Exception e){
                    logger.error( "Failed to close resource {}", name);
                }
        });
        textIO.dispose();
    }
}
