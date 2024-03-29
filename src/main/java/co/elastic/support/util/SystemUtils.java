/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.util;


import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class SystemUtils {

    private static final Logger logger = LogManager.getLogger(SystemUtils.class);

    public static void quitApp(){
        logger.info(Constants.CONSOLE,  "Exiting...");
        System.exit(0);
    }

    public static void writeToFile(String content, String dest) throws DiagnosticException {
        try {
            logger.info(Constants.CONSOLE,  "Writing to {}", dest);
            File outFile = new File(dest);
            FileUtils.writeStringToFile(outFile, content, "UTF-8");
        } catch (IOException e) {
            logger.error( "Error writing content for {}", dest, e);
            throw new DiagnosticException("Error writing " + dest + " See diagnostic.log for details.");
        }
    }

    public static void nukeDirectory(String dir){
        try {
            File tmp = new File(dir);
            tmp.setWritable(true, false);
            FileUtils.deleteDirectory(tmp);
            logger.info(Constants.CONSOLE,  "Deleted directory: {}.", dir);
        } catch (IOException e) {
            logger.error(Constants.CONSOLE, "Delete of directory:{} failed. Usually this indicates a permission issue", dir, e);
        }
    }

    public static void refreshDir(String dir){
        nukeDirectory(dir);
        File tmp = new File(dir);
        tmp.setWritable(true, false);
        tmp.mkdir();
    }

    public static String getHostName() {
        String s = null;

        BufferedReader stdInput = null;
        BufferedReader stdError = null;

        try {
            Process p = Runtime.getRuntime().exec("hostname");

            stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            s = stdInput.readLine();

        } catch (IOException e) {
            logger.error(Constants.CONSOLE,  "Error retrieving hostname.", e);
        }
        finally {
            try {
                if(stdError != null){
                    stdError.close();
                }
            } catch (IOException e) {
                logger.error(Constants.CONSOLE,  "Couldn't close stderror stream");
            }
            try {
                if(stdInput != null){
                    stdInput.close();
                }
            } catch (IOException e) {
                logger.error(Constants.CONSOLE,  "Couldn't close stdinput stream");
            }
        }

        return s;
    }

    public static Set<String> getNetworkInterfaces(){
        Set<String> ipAndHosts = new HashSet<>();

        try {
            // Get the system hostname and add it.
            String hostName = getHostName();
            ipAndHosts.add(hostName);
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();

            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                ipAndHosts.add(nic.getDisplayName());
                Enumeration<InetAddress> inets = nic.getInetAddresses();

                while (inets.hasMoreElements()) {
                    InetAddress inet = inets.nextElement();
                    ipAndHosts.add(inet.getHostAddress());
                    ipAndHosts.add(inet.getHostName());
                    ipAndHosts.add(inet.getCanonicalHostName());
                }
            }
        } catch (Exception e) {
            logger.error(Constants.CONSOLE,  "Error occurred acquiring IP's and hostnames", e);
        }

        return ipAndHosts;

    }

    public static String buildStringFromChar(int len, char seed){

        char[] charArray = new char[len];
        Arrays.fill(charArray, seed);
        return new String(charArray);

    }

    public static String getCurrentDate() {
        //DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return dtf.format(ZonedDateTime.now());
    }

    public static String getCurrentTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        return dtf.format(ZonedDateTime.now());
    }

   /**
    * This function is key for two types or execution remote and local.
    * to collect some system data or logs we need to be able to know the OS where the process is running
    * Logstash is not using this as we only collect the API data, no system data collected on the servers.
    *
    * @param  osName value provided by the Elasticsearch nodes API or Kibana stats API
    *
    * @return the OS name, as defined in our Constants
    */
    public static String parseOperatingSystemName(String osName){

        osName = osName.toLowerCase();

        if (osName.contains("windows") || osName.contains("win32")) {
            return Constants.winPlatform;

        } else if (osName.contains("linux")) {
            return Constants.linuxPlatform;

        } else if (osName.contains("darwin") || osName.contains("mac")) {
            return Constants.macPlatform;

        } else {
            // default it to Linux
            logger.warn(Constants.CONSOLE, "Unsupported OS -defaulting to Linux: {}", osName);
            return  Constants.linuxPlatform;
        }
    }

    public static boolean isRunningInDocker(){
        boolean isDocker = false;
        try {
            Path path = Paths.get(SystemProperties.userDir).getParent();
            File cgroups = new File("/proc/1/cgroup");
            
            // If it's not there we aren't in a container
            if( !cgroups.exists()){
                return false;
            }

            List<String> entries = IOUtils.readLines(new FileInputStream(cgroups), Constants.UTF_8);
            isDocker = checkCGroupEntries(entries);

        } catch (Exception e) {
            logger.error( e);
            logger.error(Constants.CONSOLE,  "Error encountered during check docker embedding. {} {}", e.getMessage(), Constants.CHECK_LOG);
            logger.error(Constants.CONSOLE,  "Assuming no container, local calls enabled.");
        }

        return isDocker;
    }



    public static boolean checkCGroupEntries(List<String> cgroups){
        for(String entry: cgroups){
            String path = entry.substring(entry.indexOf("/"));
            if(path.toLowerCase().contains("docker")){
                return true;
            }
        }
        return false;
    }
}