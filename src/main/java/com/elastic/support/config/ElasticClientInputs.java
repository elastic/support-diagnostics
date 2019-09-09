package com.elastic.support.config;

import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ElasticClientInputs extends BaseInputs {

    Logger logger = LogManager.getLogger(ElasticClientInputs.class);

    @Parameter(names = {"-h", "--host"}, required = true, description = "Required field.  Hostname, IP Address, or localhost.  HTTP access must be enabled.")
    protected String host = "";
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }

    @Parameter(names = {"--port"}, description = "HTTP or HTTPS listening port. Defaults to 9200.")
    protected int port = 9200;
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }

    @Parameter(names = {"-u", "--user"}, description = "User")
    protected String user;
    public String getUser() {
        return user;
    }
    public void setUser(String username) {
        this.user = user;
    }

    @Parameter(names = {"-p", "--password"}, description = "Password", password = true)
    protected String password;
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    @Parameter(names = {"-s", "--ssl"}, description = "Use SSL?  No value required, only the option.")
    protected boolean isSsl = false;
    public boolean isSsl() {
        return isSsl;
    }
    public String getScheme() {
        if (this.isSsl) {
            return "https";
        } else {
            return "http";
        }
    }

    @Parameter(names = {"--ptp"}, description = "Insecure plain text password - allows you to input the password as a plain text argument for scripts. WARNING: Exposes passwords in clear text. Inherently insecure.")
    protected String plainTextPassword = "";
    public String getPlainTextPassword() {
        return plainTextPassword;
    }
    public void setPlainTextPassword(String plainTextPassword) {
        this.plainTextPassword = plainTextPassword;
    }

    @Parameter(names = {"--noVerify"}, description = "Use this option to bypass hostname verification for certificate. This is inherently unsafe and NOT recommended.")
    protected boolean skipVerification = false;
    public boolean isSkipVerification() {
        return skipVerification;
    }
    public void setSkipVerification(boolean skipVerification) {
        this.skipVerification = skipVerification;
    }

    @Parameter(names = {"--keystore"}, description = "Keystore for client certificate.")
    protected String pkiKeystore;
    public String getPkiKeystore() {
        return pkiKeystore;
    }
    public void setPkiKeystore(String pkiKeystore) {
        this.pkiKeystore = pkiKeystore;
    }

    @Parameter(names = {"--keystorePass"}, description = "Keystore password for client certificate.", password = true)
    protected String pkiKeystorePass;
    public String getPkiKeystorePass() {
        return pkiKeystorePass;
    }
    public void setPkiKeystorePass(String pkiKeystorePass) {
        this.pkiKeystorePass = pkiKeystorePass;
    }

    @Parameter (names = {"--proxyHost"}, description = "Proxy server hostname.")
    protected String proxyHost;
    public String getProxyHost() {
        return proxyHost;
    }
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    @Parameter (names = {"--proxyPort"}, description = "Proxy server port.")
    protected int proxyPort = Constants.DEEFAULT_HTTP_PORT;
    public int getProxyPort() {
        return proxyPort;
    }
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    @Parameter (names = {"--proxyUser"}, description = "Proxy server user name.")
    protected String proxyUser;
    public String getProxyUser() {
        return proxyUser;
    }
    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    @Parameter (names = {"--proxyPassword"}, description = "Proxy server password.")
    protected String proxyPassword;
    public String getProxyPassword() {
        return proxyPassword;
    }
    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    @Parameter(names = {"--bypassDiagVerify"}, description = "Bypass the diagnostic version check.")
    protected boolean bypassDiagVerify = false;
    public boolean isBypassDiagVerify() {
        return bypassDiagVerify;
    }
    public void setBypassDiagVerify(boolean bypassDiagVerify) {
        this.bypassDiagVerify = bypassDiagVerify;
    }

    public boolean isSecured() {
        return (StringUtils.isNotEmpty(this.user) && StringUtils.isNotEmpty(this.password));
    }

    public boolean isPki(){
        if(StringUtils.isEmpty(pkiKeystore) ){
            return false;
        }
        return true;
    }

    public boolean validate() {

        if( ! super.validate() ){
            return false;
        }

        if (StringUtils.isNotEmpty(this.plainTextPassword)) {
            this.password = plainTextPassword;
        }

        if (! isCompleteAuth(user, password)) {
            logger.info("Input error: If authenticating both user and password are required.");
            this.jCommander.usage();
            return false;
        }

        return true;

    }

    protected boolean isCompleteAuth(String user, String password) {
        
        if ( (StringUtils.isNotEmpty(user) && StringUtils.isEmpty(password)) ||
                (StringUtils.isNotEmpty(password) && StringUtils.isEmpty(user)) ){
            return false;
        }
        return true;
    }
}
