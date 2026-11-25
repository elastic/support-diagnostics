/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.rest;

import co.elastic.support.BaseInputs;
import co.elastic.support.Constants;
import co.elastic.support.util.SystemProperties;
import co.elastic.support.util.TextIOManager;
import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ElasticRestClientInputs extends BaseInputs {

    public static final String hostDescription = "Required field.  Hostname, IP Address, or localhost.  HTTP access for this node must be enabled:";
    public static final String portDescription = "Listening port. Defaults to 9200:";
    public static final String userDescription = "Elasticsearch user account:";
    public static final String passwordDescription = "Elasticsearch user password:";
    public static final String apiKeyDescription = "Elasticsearch API key (the base64 'encoded' value, or 'id:api_key'):";
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
    public final static String  apiKeyLoginAuth = "API key:";

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
    // API key auth - same convention as user auth. Sent as "Authorization: ApiKey <key>".
    // Indicates we need to prompt for a masked input
    @Parameter(names = {"--apiKey"}, description = "Prompt for an Elasticsearch API key to authenticate with instead of user/password. Do not enter a value.")
    public boolean isApiKey = false;
    // Not shown in the the help display due to security risks - allow input via command line arguments in plain text.
    @Parameter(names = {"--apiKeyText"}, hidden = true)
    public String apiKey = "";
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

    @Override
    public List<String> parseInputs(TextIOManager textIOManager, String[] args){
        List<String> errors = super.parseInputs(textIOManager, args);
        scheme = isSsl ? "https": "http";

        errors.addAll(ObjectUtils.getIfNull(validateHost(host), List::of));
        errors.addAll(ObjectUtils.getIfNull(validateProxyHost(proxyHost), List::of));
        errors.addAll(ObjectUtils.getIfNull(TextIOManager.validateFile(pkiKeystore), List::of));
        if(StringUtils.isNotEmpty(pkiKeystore)){
            errors.addAll(ObjectUtils.getIfNull(validateAuthType(pkiLoginAuth), List::of));
        }
        errors.addAll(ObjectUtils.getIfNull(validatePort(port), List::of));
        errors.addAll(ObjectUtils.getIfNull(validatePort(proxyPort), List::of));

        // An API key replaces user/password. It may be combined with a PKI keystore, since
        // that carries the client certificate for TLS while the key authenticates the request.
        boolean apiKeyRequested = isApiKey || StringUtils.isNotEmpty(apiKey);
        List<String> combinationErrors = apiKeyRequested ? validateApiKeyCombination() : null;
        errors.addAll(ObjectUtils.getIfNull(combinationErrors, List::of));

        // If we got this far, get the passwords.
        if (isApiKey && StringUtils.isEmpty(apiKey) && combinationErrors == null) {
            apiKey = textIOManager.standardPasswordReader.read(apiKeyDescription);
        }
        if (apiKeyRequested && combinationErrors == null) {
            errors.addAll(ObjectUtils.getIfNull(validateApiKeyFormat(apiKey), List::of));
        }

        if (isPassword) {
            password = textIOManager.standardPasswordReader.read(passwordDescription);
        }

        if (StringUtils.isNotEmpty(pkiKeystore) && isPkiPass) {
            pkiKeystorePass = textIOManager.standardPasswordReader.read(pkiKeystorePasswordDescription);
        }

        if (StringUtils.isNotEmpty(proxyUser) && isProxyPass) {
            proxyPassword = textIOManager.standardPasswordReader.read(proxyPasswordDescription);
        }

        return errors;
    }

    protected void runHttpInteractive(TextIOManager textIOManager){
        if(runningInDocker){
            host = textIOManager.textIO.newStringInputReader()
                    .withMinLength(1)
                    .withIgnoreCase()
                    .withInputTrimming(true)
                    .withValueChecker((String val, String propname) -> validateHost(val))
                    .read(SystemProperties.lineSeparator + hostDescription);
        }
        else {
            host = textIOManager.textIO.newStringInputReader()
                    .withDefaultValue(host)
                    .withIgnoreCase()
                    .withInputTrimming(true)
                    .withValueChecker((String val, String propname) -> validateHost(val))
                    .read(SystemProperties.lineSeparator + hostDescription);
        }

        port = textIOManager.textIO.newIntInputReader()
                .withDefaultValue(port)
                .withValueChecker((Integer val, String propname) -> validatePort(val))
                .read(SystemProperties.lineSeparator + "Listening port. Defaults to " + port + ":");

        isSsl = textIOManager.textIO.newBooleanInputReader()
                .withDefaultValue(true)
                .read(SystemProperties.lineSeparator + sslDescription);

        if(isSsl){
            skipVerification = textIOManager.textIO.newBooleanInputReader()
                    .withDefaultValue(skipVerification)
                    .read(SystemProperties.lineSeparator + skipHostnameVerificationDescription);
        }
        else {
            scheme = "http";
        }

        boolean isSecured = textIOManager.textIO.newBooleanInputReader()
                .withDefaultValue(true)
                .read(SystemProperties.lineSeparator + "Cluster secured?");

        if(isSecured){

            String authType = textIOManager.textIO.newStringInputReader()
                    .withNumberedPossibleValues(userLoginAuth, apiKeyLoginAuth, pkiLoginAuth)
                    .withDefaultValue(userLoginAuth)
                    .read(SystemProperties.lineSeparator + "Type of authentication to use:");

            // SSL needs to be in place for PKI
            if(authType.equals(pkiLoginAuth) && !isSsl){
                textIOManager.textIO.getTextTerminal().println("TLS must be enabled to use PKI - defaulting to user/password authentication.");
                authType = userLoginAuth;
            }

            if(authType.equals(userLoginAuth)){
                user = textIOManager.standardStringReader
                        .read(SystemProperties.lineSeparator + userDescription);

                password = textIOManager.standardPasswordReader
                        .read(SystemProperties.lineSeparator + passwordDescription);
            }
            else if(authType.equals(apiKeyLoginAuth)){
                apiKey = textIOManager.standardPasswordReader
                        .read(SystemProperties.lineSeparator + apiKeyDescription);
            }
            else{
                pkiKeystore = textIOManager.standardFileReader
                        .read(SystemProperties.lineSeparator + pkiKeystoreDescription);
                pkiKeystorePass = textIOManager.standardPasswordReader
                        .read(SystemProperties.lineSeparator + pkiKeystorePasswordDescription);
            }
        }

        boolean httpProxy = textIOManager.standardBooleanReader
                .withDefaultValue(false)
                .read(SystemProperties.lineSeparator + "Http Proxy Server present?");

        if(httpProxy){
            proxyHost = textIOManager.textIO.newStringInputReader()
                    .withIgnoreCase()
                    .withInputTrimming(true)
                    .withValueChecker((String val, String propname) -> validateProxyHost(val))
                    .read(SystemProperties.lineSeparator + proxyHostDescription);

            proxyPort = textIOManager.textIO.newIntInputReader()
                    .withDefaultValue(proxyPort)
                    .withValueChecker((Integer val, String propname) -> validatePort(val))
                    .read(SystemProperties.lineSeparator + proxyPortDescription);

            proxyPassword = textIOManager.standardPasswordReader
                    .read(SystemProperties.lineSeparator + proxyPasswordDescription);
        }

    }

    public List<String> validateHost(String hostString) {

        if(StringUtils.isEmpty(hostString)){
            return List.of("Host is required.");
        }

        if(runningInDocker && Constants.localAddressList.contains(host)){
            return List.of("Local addresses are not permitted when running in a Docker container. Please use an assigned host name or IP address.");
        }

        if (hostString.toLowerCase().matches("((http|https?)://)?.*:(\\d{4,5})")) {
            return List.of("Should only contain a host name or IP Address, not a full URL");
        }
        return null;
    }

    public List<String> validateProxyHost(String hostString) {

        if(StringUtils.isNotEmpty(hostString)){
            if (hostString.toLowerCase().matches("(://)?.*:(\\d{4,5})")) {
                return List.of("Enter port separately");
            }
        }

        return null;
    }

    public List<String> validateApiKeyCombination(){
        if(StringUtils.isNotEmpty(user) || isPassword || StringUtils.isNotEmpty(password)){
            return List.of("An API key cannot be combined with a user/password. Use one or the other.");
        }
        return null;
    }

    public List<String> validateApiKeyFormat(String key){
        if(StringUtils.isBlank(key)){
            return List.of("The API key is empty.");
        }
        if(StringUtils.containsWhitespace(stripApiKeyPrefix(key))){
            return List.of("The API key must not contain whitespace or line breaks.");
        }
        return null;
    }

    public boolean usesApiKey(){
        return StringUtils.isNotBlank(apiKey);
    }

    private static String stripApiKeyPrefix(String key){
        return key.trim().replaceFirst("(?i)^apikey\\s+", "").trim();
    }

    /**
     * The value for the Authorization header. Accepts either the base64 "encoded"
     * value returned by the create API key API, or the raw "id:api_key" pair, which
     * is encoded here. Base64 never contains a colon, so the two can't be confused.
     * A leading "ApiKey " (any case) is tolerated.
     */
    public static String apiKeyHeaderValue(String key){
        String value = stripApiKeyPrefix(key);
        if(value.contains(":")){
            value = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }
        return "ApiKey " + value;
    }

    /**
     * The headers every request must carry: the configured extra headers plus,
     * when an API key was supplied, the Authorization header. Always pass this
     * (not the raw config headers) to RestClient.getClient. A configured
     * Authorization header (any case) is replaced rather than sent twice.
     */
    public Map<String, String> requestHeaders(Map<String, String> configHeaders){
        if(!usesApiKey()){
            return configHeaders;
        }
        Map<String, String> headers = new HashMap<>();
        if(configHeaders != null){
            headers.putAll(configHeaders);
        }
        headers.keySet().removeIf(name -> "Authorization".equalsIgnoreCase(name));
        headers.put("Authorization", apiKeyHeaderValue(apiKey));
        return headers;
    }

    public List<String> validateAuthType(String val){
        if( val.equals(pkiLoginAuth) && !isSsl){
            return List.of("TLS must be enabled to use PKI Authentication.");
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
                ", isApiKey=" + isApiKey +
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
