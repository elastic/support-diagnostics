package com.elastic.support.rest;

import com.beust.jcommander.Parameter;
import com.elastic.support.BaseInputs;
import com.elastic.support.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;

public class ElasticRestClientInputs extends BaseInputs {

    Logger logger = LogManager.getLogger(ElasticRestClientInputs.class);

    // The basics - where's the cluster
    @Parameter(names = {"-h", "--host"}, description = "Required field.  Hostname, IP Address, or localhost.  HTTP access must be enabled.")
    public String host = "";
    @Parameter(names = {"--port"}, description = "HTTP or HTTPS listening port. Defaults to 9200.")
    public int port = 9200;

    // Proxy Server settings
    @Parameter(names = {"--proxyHost"}, description = "Proxy server hostname.")
    public String proxyHost;
    @Parameter(names = {"--proxyPort"}, description = "Proxy server port.")
    public int proxyPort = Constants.DEEFAULT_HTTP_PORT;

    // Secured authentication logins
    // Switch just turns on the prompt - they can still send it in as a parameter
    // but it won't show in the usage, just the readme.
    // Rest of the password fields follow the same convention.
    @Parameter(names = {"-u", "--user"}, description = "User")
    public String user;
    @Parameter(names = {"-p", "--password"}, description = "Prompt for Elasticsearch password", password = true )
    public boolean passwordSwitch = false;
    @Parameter(names = { "--pwdText"}, hidden = true)
    public String password;

    // PKI auth - same convention as user auth.
    @Parameter(names = {"--keystore"}, description = "Path/filename for keystore with client certificate.")
    public String pkiKeystore;
    @Parameter(names = {"--keystorePass"}, description = "Prompt for keystore password.")
    public boolean keystorePasswordSwitch = false;
    @Parameter(names = { "--ksPwdText"}, hidden = true)
    public String pkiKeystorePass;

    // Authebnticated proxies
    @Parameter(names = {"--proxyUser"}, description = "Proxy server user name.")
    public String proxyUser;
    @Parameter(names = {"--proxyPassword"})
    public boolean proxyPasswordSwitch = false;
    @Parameter(names = { "--proxyPwdText"}, hidden = true)
    public String proxyPassword;

    // SSL and hostname verification switches
    @Parameter(names = {"-s", "--ssl"}, description = "Use SSL?  No value required, only the option.")
    public boolean isSsl = false;
    @Parameter(names = {"--noVerify"}, description = "Use this option to bypass hostname verification for certificate. This is inherently unsafe and NOT recommended.")
    public boolean skipVerification = false;

    // Stop the diag from checking itself for latest version.
    @Parameter(names = {"--bypassDiagVerify"}, description = "Bypass the diagnostic version check. Use when internet outbound HTTP access is blocked by a firewall.")
    public boolean bypassDiagVerify = false;

    public String getScheme() {
        if (this.isSsl) {
            return "https";
        } else {
            return "http";
        }
    }

    public boolean validate() {

        boolean noErrors = true;

        if (!super.validate()) {
            noErrors = false;
        }

        if (! validateHost(host)) {
            noErrors = false;
            messages.add("A non-empty host in the proper format must be entered.");
        }

        if ( StringUtils.isNotEmpty(user) && StringUtils.isEmpty(password) ) {
            messages.add("Password is required and must be at least 6 characters when login is specified.");
            noErrors =  false;
        }

        if (StringUtils.isNotEmpty(password) && StringUtils.isEmpty(user)) {
            messages.add("User is required when a password is specified.");
            noErrors = false;
        }

        if (StringUtils.isNotEmpty(proxyPassword) && StringUtils.isEmpty(proxyUser)) {
            messages.add("Proxy user is required when a proxy password is specified.");
            noErrors = false;
        }

        if (StringUtils.isNotEmpty(pkiKeystore)) {
            File pki = new File(pkiKeystore);
            if (!pki.exists()) {
                messages.add("A PKI Keystore was input but could not be located in the supplied location. Please check that the absolute path to the file is correct.");
                noErrors = false;
            }
        }

        return noErrors;

    }

    public void promptHost(){
        boolean validHost = false;
        while (! validHost) {
            System.out.println("");
            System.out.println("Enter a hostname or IP address for one of the nodes in the cluster.");
            System.out.println("This node must be accessible via HTTP.");

            String input = consoleInput("Enter a value or <Enter Key> to use localhost.");
            validHost = validateHost(input);
            if(validHost) {
                host = input;
            }
            else{
                System.out.println("Please enter a hostname rather than a full URL.");
            }
        }
    }

    public void promptPort(){
        System.out.println("");
        boolean validPort = false;
        while (! validPort){
            System.out.println("Default ports - Elasticsearch: 9200, Logstash: 9600");
            String tempPort =  console.readLine("Enter HTTP port target or <Enter Key> for default: ");
            if(StringUtils.isEmpty(tempPort)){
                break;
            }
            if(! NumberUtils.isDigits(tempPort)){
                System.out.println("Port value must be a 4-5 digit number.");
                continue;
            }
            port = Integer.parseInt(tempPort);
            validPort = true;
        }
    }

    public String promptUser(){
        System.out.println("");
        return consoleInput("Enter Elasticsearch user login: ");
    }
    public void promptPassword(){
        System.out.println("");
        System.out.println("Authentication requires a password which must be at least 6 characters.");
        password = consolePassword("Enter password: ");
    }

    public void promptSsl(){
        System.out.println("");
        while(true) {
            String input = consoleInput("Use HTTPS? Y|N <Enter Key> for default N: ");
            try {
                isSsl = convertYesNoStringInput(input);
                break;
            } catch (IllegalArgumentException iae) {
                System.out.println(BaseInputs.YesNoValidationMessage);
            }
        }
    }
    public void promptSkipVerification(){
        System.out.println("");
        System.out.println("Should the utility verify the host name for the certificate used in the HTTPS connection?");
        System.out.println("Bypassing this check is unsafe and not recommended.");

        boolean isValid = false;
        while(! isValid) {
            String input = consoleInput("Bypass verification? Y|N <Enter Key> for default N: ");
            input = input.trim().toLowerCase();
            if (input.equals("y") || input.equals("yes") || input.equals("t") || input.equals("true")) {
                skipVerification = true;
                isValid = true;
            } else if (input.equals("n") || input.equals("no") || input.equals("f") || input.equals("false")) {
                skipVerification = false;
                isValid = true;
            }
            else if(input.equals("")){
                skipVerification = false;
                isValid = true;
            }
            else {
                System.out.println("Valid responses: Y, N or hit <Enter Key> for N.");
            }
        }
    }
    
    public void promptPkiStore(){
        pkiKeystore = consoleInput("Enter the full path to a keystore to be used for PKI Authentication or <Enter Key> for none: ");
    }
    public void promptPkiPassword(){
        pkiKeystorePass = consolePassword("Enter the password for the PKI Auth keytstore or <Enter Key> for none: ");
    }
    
    public void promptProxyHost(){
        proxyHost = consoleInput("Enter a proxy host name or IP or <Enter Key> to bypass");
    }
    public void promptProxyPort(){
        if(StringUtils.isNotEmpty(proxyHost)){
            String temp = "";
            while(StringUtils.isEmpty(temp)) {
                temp = consoleInput("Enter a proxy port number. Required if proxy host is set: ");
                if (NumberUtils.isDigits(temp)) {
                    proxyPort = Integer.parseInt(temp);
                    break;
                }
                temp = "";
            }
        }
    }
    public void promptProxyUser(){
        proxyUser = consoleInput("User login for the specified proxy server: ");
    }
    public void promptProxyPassword(){
        proxyUser = consolePassword("Password for the specified proxy user: ");
    }
    public void promptBypassDiagVerification(){
        boolean isValid = false;
        while(! isValid) {
            String input = consoleInput("Bypass verification? Y|N <Enter Key> for default N: ");
            input = input.trim().toLowerCase();
            if (input.equals("y") || input.equals("yes") || input.equals("t") || input.equals("true")) {
                bypassDiagVerify = true;
                isValid = true;
            } else if (input.equals("n") || input.equals("no") || input.equals("f") || input.equals("false")) {
                bypassDiagVerify = false;
                isValid = true;
            }
            else if(input.equals("")){
                bypassDiagVerify = false;
                isValid = true;
            }
            else {
                System.out.println("Valid responses: Y, N or hit <Enter Key> for N.");
            }
        }
    }

    public boolean validateHost(String hostString){
        // Returns false if empty or a full url:port combination
        if(StringUtils.isEmpty(hostString)){
            return false;
        }
       return ! hostString.toLowerCase().matches("((http|https?)://)?.*:(\\d*)");
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
                ", bypassDiagVerify=" + bypassDiagVerify +
                '}';
    }
}
