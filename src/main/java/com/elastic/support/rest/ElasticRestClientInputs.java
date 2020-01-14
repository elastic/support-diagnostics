package com.elastic.support.rest;

import com.beust.jcommander.Parameter;
import com.elastic.support.BaseInputs;
import com.elastic.support.Constants;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.beryx.textio.BooleanInputReader;
import org.beryx.textio.IntInputReader;
import org.beryx.textio.StringInputReader;

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
    public String proxyHost;
    @Parameter(names = {"--proxyPort"}, description = proxyPortDescription)
    public int proxyPort = Constants.DEEFAULT_PROXY_PORT;
    // Secured authentication logins
    // Switch just turns on the prompt - they can still send it in as a parameter
    // but it won't show in the usage, just the readme.
    // Rest of the password fields follow the same convention.
    @Parameter(names = {"-u", "--user"}, description = userDescription)
    public String user;
    // Indicates we need to prompt for a masked input
    @Parameter(names = {"-p", "--password"}, description = "Prompt for Elasticsearch password.  Do not enter a value.")
    public boolean passwordSwitch = false;
    // Not shown in the the help display due to security risks - allow input via command line arguments in plain text.
    @Parameter(names = {"--pwdText"}, hidden = true)
    public String password;
    // PKI auth - same convention as user auth.
    @Parameter(names = {"--keystore"}, description = pkiKeystoreDescription)
    public String pkiKeystore;
    // Indicates we need to prompt for a masked input
    @Parameter(names = {"--keystorePass"}, description = "Prompt for keystore password. Do not enter a value.")
    public boolean keystorePasswordSwitch = false;
    // Not shown in the the help display due to security risks - allow input via command line arguments in plain text.
    @Parameter(names = {"--ksPwdText"}, hidden = true)
    public String pkiKeystorePass;
    // Authenticated proxies
    @Parameter(names = {"--proxyUser"}, description = proxyUserDescription)
    public String proxyUser;
    // Indicates we need to prompt for a masked input
    @Parameter(names = {"--proxyPassword"}, description = "Prompt for proxy password. Do not enter a value.")
    public boolean proxyPasswordSwitch = false;
    // Not shown in the the help display due to security risks - allow input via command line arguments in plain text.
    @Parameter(names = {"--proxyPwdText"}, hidden = true)
    public String proxyPassword;
    // SSL and hostname verification switches
    @Parameter(names = {"-s", "--ssl"}, description = sslDescription + useOptionOnly )
    public boolean isSsl = false;
    @Parameter(names = {"--noVerify"}, description = bypassDiagVerifyDescription + useOptionOnly)
    public boolean skipVerification = false;

    // Other fields
    public String scheme = "https";

    // End Input Fields

    // Start Input Readers

    // Generic - change the default values and the read label only
    protected StringInputReader standardSteringReader = textIO.newStringInputReader()
            .withInputTrimming(true);
    protected BooleanInputReader standardBooleanReader = textIO.newBooleanInputReader();
    protected StringInputReader  standardPasswordReader = textIO.newStringInputReader()
            .withInputMasking(true)
            .withInputTrimming(true);
    protected StringInputReader standardFileReader = textIO.newStringInputReader()
            .withInputTrimming(true)
            .withValueChecker((String val, String propname) -> validateFile(val));
    protected IntInputReader standardPortReader = textIO.newIntInputReader()
            .withValueChecker((Integer val, String propname) -> validatePort(val));

    protected StringInputReader hostReader = textIO.newStringInputReader()
            .withDefaultValue(host)
            .withIgnoreCase()
            .withInputTrimming(true)
            .withValueChecker((String val, String propname) -> validateHost(val));

    protected IntInputReader portReader = textIO.newIntInputReader()
            .withDefaultValue(port)
            .withMaxVal(65535)
            .withMinVal(1);

    protected BooleanInputReader schemeReader = textIO.newBooleanInputReader()
            .withValueChecker((Boolean val, String propname) -> validateSsl(val));

    protected StringInputReader userReader = textIO.newStringInputReader()
            .withInputTrimming(true);

    protected StringInputReader passwordReader = textIO.newStringInputReader()
            .withMinLength(6)
            .withInputMasking(true)
            .withInputTrimming(true)
            .withValueChecker((String val, String propname) -> validatePassword(val));

    protected StringInputReader authTypeReader = textIO.newStringInputReader()
            .withNumberedPossibleValues(userLoginAuth, pkiLoginAuth)
            .withDefaultValue(userLoginAuth);

    protected StringInputReader proxyHostReader = textIO.newStringInputReader()
            .withIgnoreCase()
            .withInputTrimming(true)
            .withValueChecker((String val, String propname) -> validateProxyHost(val));


    // End Input Readers
    Logger logger = LogManager.getLogger(ElasticRestClientInputs.class);

    public List<String> validate() {

        List<String> errors = new ArrayList<>();
        errors.addAll(ObjectUtils.defaultIfNull(super.validate(), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validateSsl(isSsl), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validateHost(host), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validateProxyHost(proxyHost), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validatePassword(password), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validateFile(pkiKeystore), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validatePort(port), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validatePort(proxyPort), emptyList));
        if(StringUtils.isNotEmpty(pkiKeystore)){
            errors.addAll(ObjectUtils.defaultIfNull(validateAuthType(pkiLoginAuth), emptyList));
        }

        if(errors.size() > 0){
            return errors;
        }

        // If we got this far, get the passwords.
        if(passwordSwitch){
            password = passwordReader
                    .read(passwordDescription);
        }

        if(keystorePasswordSwitch){
            pkiKeystorePass = standardPasswordReader
                    .read(pkiKeystorePasswordDescription);
        }

        if(proxyPasswordSwitch){
            proxyPassword = standardPasswordReader
                    .read(proxyPasswordDescription);
        }

        return errors;

    }

    public List<String> validateHost(String hostString) {

        if (hostString.toLowerCase().matches("((http|https?)://)?.*:(\\d{4,5})")) {
            return Collections.singletonList("Should only contain a host name or IP Address, not a full URL");
        }
        return null;
    }

    public List<String> validateProxyHost(String hostString) {

        if (hostString.toLowerCase().matches("(://)?.*:(\\d{4,5})")) {
            return Collections.singletonList("Enter port separately");
        }
        return null;
    }

    public List<String> validatePort(int val){
        if (val < 1 || val > 65535){
            return Collections.singletonList("Outside the valid range of port values. 1-65535 ");
        }
        return null;
    }

    public List<String> validatePassword(String val) {
        if (StringUtils.isEmpty(user)) {
            return Collections.singletonList("Password cannot be associated with an empty user id");
        }
        return null;
    }

    public List<String> validateFile(String val) {
        if (StringUtils.isEmpty(val.trim())) {
            return emptyList;
        }

        File file = new File(val);

        if (!file.exists()) {
            return Collections.singletonList("Specified file could not be located.");
        }

        return null;

    }

    public List<String> validateSsl(boolean ssl) {
        // We're just setting the scheme here so it stays consistent with the legacy command line
        if (ssl) {
            scheme = "https";
        } else {
            scheme = "http";
        }

        // Never an error.
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
