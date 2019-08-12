package com.elastic.support.util;


import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.converters.BooleanConverter;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemUtils {

    private static final Logger logger = LoggerFactory.getLogger(SystemUtils.class);


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