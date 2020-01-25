package com.elastic.support.rest;

import com.beust.jcommander.Parameter;
import com.elastic.support.BaseInputs;
import com.elastic.support.Constants;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.beryx.textio.BooleanInputReader;
import org.beryx.textio.IntInputReader;
import org.beryx.textio.StringInputReader;
import org.beryx.textio.TextTerminal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ElasticRestClientInputs extends BaseInputs {

    public static final String hostDescription = "Required field.  Hostname, IP Address, or localhost.  HTTP access for this node must be enabled.";
    public static final String portDescription = "HTTP or HTTPS listening port. Defaults to 9200.";
    public static final String userDescription = "Elasticsearch user account";
    public static final String passwordDescription = "Elasticsearch user password";
    public static final String pkiKeystoreDescription = "Path/filename for PKI keystore with client certificate.";
    public static final String pkiKeystorePasswordDescription = "PKI keystore password if required.";
    public static final String proxyHostDescription = "Proxy server host name or IP Address.";
    public static final String proxyPortDescription = "Proxy server port number. Defaults to port 80.";
    public static final String proxyUserDescription = "Proxy server login.";
    public static final String proxyPasswordDescription = "Proxy server password if required.";
    public static final String sslDescription = "TLS enabled, use HTTPS?";
    public static final String skipHostnameVerificationDescription = "Bypass hostname verification for certificate? This is inherently unsafe and NOT recommended.";

    public final static String  userLoginAuth = "Username/Password";
    public final static String  pkiLoginAuth = "PKI";

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
    public boolean passwordSwitch = false;
    // Not shown in the the help display due to security risks - allow input via command line arguments in plain text.
    @Parameter(names = {"--pwdText"}, hidden = true)
    public String password = "";
    // PKI auth - same convention as user auth.
    @Parameter(names = {"--keystore"}, description = pkiKeystoreDescription)
    public String pkiKeystore = "";
    // Indicates we need to prompt for a masked input
    //@Parameter(names = {"--pkiPass"}, description = "Prompt for keystore password. Do not enter a value.")
    //public boolean keystorePasswordSwitch = false;*/
    // Not shown in the the help display due to security risks - allow input via command line arguments in plain text.
    @Parameter(names = {"--ksPwdText"}, hidden = true)
    public String pkiKeystorePass = "";
    // Authenticated proxies
    @Parameter(names = {"--proxyUser"}, description = proxyUserDescription)
    public String proxyUser = "";
    // Indicates we need to prompt for a masked input
    @Parameter(names = {"--proxyPass"}, description = "Prompt for proxy password. Do not enter a value.", hidden = true)
    public boolean proxyPasswordSwitch = false;
    // Not shown in the the help display due to security risks - allow input via command line arguments in plain text.
    @Parameter(names = {"--proxyPwdText"}, hidden = true)
    public String proxyPassword = "";
    // SSL and hostname verification switches
    @Parameter(names = {"-s", "--ssl"}, description = sslDescription + useOptionOnly )
    public boolean isSsl = false;
    @Parameter(names = {"--noVerify"}, description = bypassDiagVerifyDescription + useOptionOnly)
    public boolean skipVerification = false;

    // Other fields
    public String scheme = "https";

    // End Input Fields

    // Start Input Readers

    protected StringInputReader hostReader = textIO.newStringInputReader()
            .withDefaultValue(host)
            .withIgnoreCase()
            .withInputTrimming(true)
            .withValueChecker((String val, String propname) -> validateHost(val));

    protected StringInputReader authTypeReader = textIO.newStringInputReader()
            .withNumberedPossibleValues(userLoginAuth, pkiLoginAuth)
            .withDefaultValue(userLoginAuth);

    protected StringInputReader proxyHostReader = textIO.newStringInputReader()
            .withIgnoreCase()
            .withInputTrimming(true)
            .withValueChecker((String val, String propname) -> validateProxyHost(val));


    // End Input Readers
    Logger logger = LogManager.getLogger(ElasticRestClientInputs.class);

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
        if(passwordSwitch){
            if(StringUtils.isEmpty(password)){
                password = standardPasswordReader
                        .read(passwordDescription);
            }
        }

        if(StringUtils.isNotEmpty(pkiKeystore)){
            if(StringUtils.isEmpty(pkiKeystorePass)){
                pkiKeystorePass = standardPasswordReader
                        .read(pkiKeystorePasswordDescription);
            }
        }

        if(StringUtils.isNotEmpty(proxyUser)){
            if(StringUtils.isEmpty(proxyPassword)){
                proxyPassword = standardPasswordReader
                        .read(proxyPasswordDescription);
            }
        }

        return errors;

    }

    protected void runHttpInteractive(){

        host = hostReader
                .read(SystemProperties.lineSeparator + hostDescription);

        port = standardPortReader
                .withDefaultValue(port)
                .read(SystemProperties.lineSeparator + portDescription);

        isSsl = standardBooleanReader
                .withDefaultValue(true)
                .read(SystemProperties.lineSeparator + sslDescription);

        if(isSsl){
            skipVerification = standardBooleanReader
                    .withDefaultValue(skipVerification)
                    .read(SystemProperties.lineSeparator + skipHostnameVerificationDescription);
        }

        boolean isSecured = standardBooleanReader
                .withDefaultValue(true)
                .read(SystemProperties.lineSeparator + "Cluster secured?");

        if(isSecured){
            user = standardStringReader
                    .read(SystemProperties.lineSeparator + userDescription);

            String authType = authTypeReader
                    .read(SystemProperties.lineSeparator + "Type of authentication to use:");

            // SSL needs to be in place for PKI
            if(authType.equals(pkiLoginAuth) && !isSsl){
                terminal.println("TLS must be enabled to use PKI - defaulting to user/password authentication.");
                authType = userLoginAuth;
            }

            if(authType.equals(userLoginAuth)){
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
            proxyHost = proxyHostReader
                    .read(SystemProperties.lineSeparator + proxyHostDescription);

            proxyPort = standardPortReader
                    .withDefaultValue(proxyPort)
                    .read(SystemProperties.lineSeparator + proxyPortDescription);

            proxyPassword = standardPasswordReader
                    .read(SystemProperties.lineSeparator + proxyPasswordDescription);
        }

    }

    public List<String> validateHost(String hostString) {

        if(StringUtils.isEmpty(hostString)){
            return Collections.singletonList("Host is required.");
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

        return superString + "," + "ElasticRestClientInputs{" +
                "logger=" + logger +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", user='" + user + '\'' +
                ", isSsl=" + isSsl +
                ", skipVerification=" + skipVerification +
                ", pkiKeystore='" + pkiKeystore + '\'' +
                ", proxyHost='" + proxyHost + '\'' +
                ", proxyPort=" + proxyPort +
                ", proxyUser='" + proxyUser + '\'' +
                '}';
    }
}
