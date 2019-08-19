package com.elastic.support.util;


import com.elastic.support.config.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


public class SystemUtils {

    private static final Logger logger = LogManager.getLogger(SystemUtils.class);

    public static void toFile(String path, String content) {
        try (FileOutputStream fs = new FileOutputStream(path)) {
            IOUtils.write(content, fs, Constants.UTF8);
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Error writing Response To OutputStream", e);
        }
    }

    public static void streamClose(String path, InputStream instream) {

        if (instream != null) {
            try {
                instream.close();
            } catch (Throwable t) {
                logger.error("Error encountered when attempting to close file {}", path);
            }
        } else {
            logger.error("Error encountered when attempting to close file: null InputStream {}", path);
        }

    }

    public static void nukeDirectory(String dir){
        try {
            File tmp = new File(dir);
            tmp.setWritable(true, false);
            FileUtils.deleteDirectory(tmp);
            logger.info("Deleted directory: {}.", dir);
        } catch (IOException e) {
            logger.error("Access issue with target directory", e);
        }
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
            logger.error("Error retrieving hostname.", e);
        }
        finally {
            try {
                if(stdError != null){
                    stdError.close();
                }
            } catch (IOException e) {
                logger.error("Couldn't close stderror stream");
            }
            try {
                if(stdInput != null){
                    stdInput.close();
                }
            } catch (IOException e) {
                logger.error("Couldn't close stdinput stream");
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
            logger.error("Error occurred acquiring IP's and hostnames", e);
        }

        return ipAndHosts;

    }

    public static String buildStringFromChar(int len, char seed){

        char[] charArray = new char[len];
        Arrays.fill(charArray, seed);
        return new String(charArray);

    }



}