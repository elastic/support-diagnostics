/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package com.elastic.support.rest;

import com.beust.jcommander.Parameter;
import com.elastic.support.BaseInputs;
import com.elastic.support.Constants;
import com.elastic.support.util.ResourceCache;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Collections;
import java.util.List;

public abstract class ElasticRestClientInputs extends BaseInputs {

    public static final String hostDescription = "Required field.  Hostname, IP Address, or localhost.  HTTP access for this node must be enabled:";
    public static final String portDescription = "Listening port. Defaults to 9200:";
    public static final String userDescription = "Elasticsearch user account:";
    public static final String passwordDescription = "Elasticsearch user password:";
    public static final String pkiKeystoreDescription = "Path/filename for PKI keystore with client certificate:";
    public static final String pkiKeystorePasswordDescription = "PKI keystore password if required:";
    public static final String proxyHostDescription = "Proxy server host name or IP Address:";
    public static final String proxyPortDescription = "Proxy server port number. Defaults to port 80:";
    public static final String proxyUserDescription = "Proxy server login:";
    public static final String proxyPasswordDescription = "Proxy server password if required:";
    public static final String sslDescription = "Use https to access the cluster?";
    public static final String skipHostnameVerificationDescription = "Bypass hostname verification for certificate? This is unsafe and NOT recommended.";

    public final static String  userLoginAuth = "Username/Password:";
    public final static String  pkiLoginAuth = "PKI:";

    public static final String useOptionOnly = " No value required, only the option.";
    // The basics - where's the cluster
    @Parameter(names = {"-h", "--host"}, description = hostDescription)
    public String host = "localhost";

    // Start Input Fields
    @Parameter(names = {"--port"}, description = portDescription)
    public int port = 9200;
    // Proxy Server settings
    @Parameter(names = {"--proxyHost"}, description = proxyHostDescription)
    public String proxyHost = "";
    @Parameter(names = {"--proxyPort"}, description = proxyPortDescription)
    public int proxyPort = Constants.DEEFAULT_PROXY_PORT;
    // Secured authentication logins
    // Switch just turns on the prompt - they can still send it in as a parameter
    // but it won't show in the usage, just the readme.
    // Rest of the password fields follow the same convention.
    @Parameter(names = {"-u", "--user"}, description = userDescription)
    public String user = "";
    // Indicates we need to prompt for a masked input
    @Parameter(names = {"-p", "--password"}, description = "Prompt for Elasticsearch password.  Do not enter a value.")
    public boolean isPassword = false;
    // Not shown in the the help display due to security risks - allow input via command line arguments in plain text.
    @Parameter(names = {"--passwordText"}, hidden = true)
    public String password = "";
    // PKI auth - same convention as user auth.
    @Parameter(names = {"--pkiKeystore"}, description = pkiKeystoreDescription)
    public String pkiKeystore = "";
    // Indicates we need to prompt for a masked input
    @Parameter(names = {"--pkiPass"}, description = "Prompt for keystore password. Do not enter a value.")
    public boolean isPkiPass = false;
    // Not shown in the the help display due to security risks - allow input via command line arguments in plain text.
    @Parameter(names = {"--pkiPassText"}, hidden = true)
    public String pkiKeystorePass = "";
    // Authenticated proxies
    @Parameter(names = {"--proxyUser"}, description = proxyUserDescription)
    public String proxyUser = "";
    // Indicates we need to prompt for a masked input
    @Parameter(names = {"--proxyPass"}, description = "Prompt for proxy password. Do not enter a value.", hidden = true)
    public boolean isProxyPass = false;
    // Not shown in the the help display due to security risks - allow input via command line arguments in plain text.
    @Parameter(names = {"--proxyPassText"}, hidden = true)
    public String proxyPassword = "";
    // SSL and hostname verification switches
    @Parameter(names = {"-s", "--ssl"}, description = sslDescription + useOptionOnly )
    public boolean isSsl = false;
    @Parameter(names = {"--noVerify"}, description = skipHostnameVerificationDescription + useOptionOnly)
    public boolean skipVerification = false;

    // Other fields
    public String scheme = "https";

    // End Input Fields

    Logger logger = LogManager.getLogger(ElasticRestClientInputs.class);

    public ElasticRestClientInputs(){
        if(runningInDocker){
            host = "";
        }
    }

    public List<String> parseInputs(String args[]){
        List<String> errors = super.parseInputs(args);
        scheme = isSsl ? "https": "http";

        errors.addAll(ObjectUtils.defaultIfNull(validateHost(host), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validateProxyHost(proxyHost), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validateFile(pkiKeystore), emptyList));
        if(StringUtils.isNotEmpty(pkiKeystore)){
            errors.addAll(ObjectUtils.defaultIfNull(validateAuthType(pkiLoginAuth), emptyList));
        }
        errors.addAll(ObjectUtils.defaultIfNull(validatePort(port), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validatePort(proxyPort), emptyList));

        // If we got this far, get the passwords.
        if(isPassword){
                password = standardPasswordReader
                        .read(passwordDescription);
        }

        if(StringUtils.isNotEmpty(pkiKeystore)){
            if(isPkiPass){
                pkiKeystorePass = standardPasswordReader
                        .read(pkiKeystorePasswordDescription);
            }
        }

        if(StringUtils.isNotEmpty(proxyUser)){
            if(isProxyPass){
                proxyPassword = standardPasswordReader
                        .read(proxyPasswordDescription);
            }
        }

        return errors;

    }

    protected void runHttpInteractive(){

        if(runningInDocker){
            host = ResourceCache.textIO.newStringInputReader()
                    .withMinLength(1)
                    .withIgnoreCase()
                    .withInputTrimming(true)
                    .withValueChecker((String val, String propname) -> validateHost(val))
                    .read(SystemProperties.lineSeparator + hostDescription);
        }
        else {
            host = ResourceCache.textIO.newStringInputReader()
                    .withDefaultValue(host)
                    .withIgnoreCase()
                    .withInputTrimming(true)
                    .withValueChecker((String val, String propname) -> validateHost(val))
                    .read(SystemProperties.lineSeparator + hostDescription);
        }

        port = ResourceCache.textIO.newIntInputReader()
                .withDefaultValue(port)
                .withValueChecker((Integer val, String propname) -> validatePort(val))
                .read(SystemProperties.lineSeparator + "Listening port. Defaults to " + port + ":");

        isSsl = ResourceCache.textIO.newBooleanInputReader()
                .withDefaultValue(true)
                .read(SystemProperties.lineSeparator + sslDescription);

        if(isSsl){
            skipVerification = ResourceCache.textIO.newBooleanInputReader()
                    .withDefaultValue(skipVerification)
                    .read(SystemProperties.lineSeparator + skipHostnameVerificationDescription);
        }
        else {
            scheme = "http";
        }

        boolean isSecured = ResourceCache.textIO.newBooleanInputReader()
                .withDefaultValue(true)
                .read(SystemProperties.lineSeparator + "Cluster secured?");

        if(isSecured){

            String authType = ResourceCache.textIO.newStringInputReader()
                    .withNumberedPossibleValues(userLoginAuth, pkiLoginAuth)
                    .withDefaultValue(userLoginAuth)
                    .read(SystemProperties.lineSeparator + "Type of authentication to use:");

            // SSL needs to be in place for PKI
            if(authType.equals(pkiLoginAuth) && !isSsl){
                ResourceCache.terminal.println("TLS must be enabled to use PKI - defaulting to user/password authentication.");
                authType = userLoginAuth;
            }

            if(authType.equals(userLoginAuth)){
                user = standardStringReader
                        .read(SystemProperties.lineSeparator + userDescription);

                password = standardPasswordReader
                        .read(SystemProperties.lineSeparator + passwordDescription);
            }
            else{
                pkiKeystore = standardFileReader
                        .read(SystemProperties.lineSeparator + pkiKeystoreDescription);
                pkiKeystorePass = standardPasswordReader
                        .read(SystemProperties.lineSeparator + pkiKeystorePasswordDescription);
            }
        }

        boolean httpProxy = standardBooleanReader
                .withDefaultValue(false)
                .read(SystemProperties.lineSeparator + "Http Proxy Server present?");

        if(httpProxy){
            proxyHost = ResourceCache.textIO.newStringInputReader()
                    .withIgnoreCase()
                    .withInputTrimming(true)
                    .withValueChecker((String val, String propname) -> validateProxyHost(val))
                    .read(SystemProperties.lineSeparator + proxyHostDescription);

            proxyPort = ResourceCache.textIO.newIntInputReader()
                    .withDefaultValue(proxyPort)
                    .withValueChecker((Integer val, String propname) -> validatePort(val))
                    .read(SystemProperties.lineSeparator + proxyPortDescription);

            proxyPassword = standardPasswordReader
                    .read(SystemProperties.lineSeparator + proxyPasswordDescription);
        }

    }

    public List<String> validateHost(String hostString) {

        if(StringUtils.isEmpty(hostString)){
            return Collections.singletonList("Host is required.");
        }

        if(runningInDocker && Constants.localAddressList.contains(host)){
            return Collections.singletonList("Local addresses are not permitted when running in a Docker container. Please use an assigned host name or IP address.");
        }

        if (hostString.toLowerCase().matches("((http|https?)://)?.*:(\\d{4,5})")) {
            return Collections.singletonList("Should only contain a host name or IP Address, not a full URL");
        }
        return null;
    }

    public List<String> validateProxyHost(String hostString) {

        if(StringUtils.isNotEmpty(hostString)){
            if (hostString.toLowerCase().matches("(://)?.*:(\\d{4,5})")) {
                return Collections.singletonList("Enter port separately");
            }
        }

        return null;
    }

    public List<String> validateAuthType(String val){
        if( val.equals(pkiLoginAuth) && !isSsl){
            return Collections.singletonList("TLS must be enabled to use PKI Authentication.");
        }
        return null;
    }

    @Override
    public String toString() {

        String superString = super.toString();
        return superString + "," +
        "ElasticRestClientInputs: {" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", proxyHost='" + proxyHost + '\'' +
                ", proxyPort=" + proxyPort +
                ", user='" + user + '\'' +
                ", isPassword=" + isPassword +
                ", pkiKeystore='" + pkiKeystore + '\'' +
                ", isPkiPass=" + isPkiPass +
                ", pkiKeystorePass='" + pkiKeystorePass + '\'' +
                ", proxyUser='" + proxyUser + '\'' +
                ", isProxyPass=" + isProxyPass +
                ", isSsl=" + isSsl +
                ", skipVerification=" + skipVerification +
                ", scheme='" + scheme + '\'' +
                '}';
    }
}
