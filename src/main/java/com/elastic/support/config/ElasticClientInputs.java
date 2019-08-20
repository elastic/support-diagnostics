package com.elastic.support.config;

import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ElasticClientInputs extends BaseInputs {


    Logger logger = LogManager.getLogger(ElasticClientInputs.class);

    @Parameter(names = {"-h", "--host"}, description = "Required field.  Hostname, IP Address, or localhost.  HTTP access must be enabled.")
    protected String host = "";
    @Parameter(names = {"--port"}, description = "HTTP or HTTPS listening port. Defaults to 9200.")
    protected int port = 9200;
    @Parameter(names = {"-u", "--user"}, description = "User")
    protected String user;
    @Parameter(names = {"-p", "--password"}, description = "Password", password = true)
    protected String password;
    @Parameter(names = {"-s", "--ssl"}, description = "Use SSL?  No value required, only the option.")
    protected boolean isSsl = false;
    @Parameter(names = {"--ptp"}, description = "Insecure plain text password - allows you to input the password as a plain text argument for scripts. WARNING: Exposes passwords in clear text. Inherently insecure.")
    protected String plainTextPassword = "";
    @Parameter(names = {"--noVerify"}, description = "Use this option to bypass hostname verification for certificate. This is inherently unsafe and NOT recommended.")
    protected boolean skipVerification = false;
    @Parameter(names = {"--keystore"}, description = "Keystore for client certificate.")
    protected String pkiKeystore;
    @Parameter(names = {"--keystorePass"}, description = "Keystore password for client certificate.", password = true)
    protected String pkiKeystorePass;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String username) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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

    public String getPlainTextPassword() {
        return plainTextPassword;
    }

    public void setPlainTextPassword(String plainTextPassword) {
        this.plainTextPassword = plainTextPassword;
    }

    public boolean isSkipVerification() {
        return skipVerification;
    }

    public void setSkipVerification(boolean skipVerification) {
        this.skipVerification = skipVerification;
    }

    public String getPkiKeystore() {
        return pkiKeystore;
    }

    public void setPkiKeystore(String pkiKeystore) {
        this.pkiKeystore = pkiKeystore;
    }

    public String getPkiKeystorePass() {
        return pkiKeystorePass;
    }

    public void setPkiKeystorePass(String pkiKeystorePass) {
        this.pkiKeystorePass = pkiKeystorePass;
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

        if (!isAuthValid(user, password)) {
            logger.info("Input error: If authenticating both user and password are required.");
            this.jCommander.usage();
            return false;
        }

        return true;

    }

    protected boolean isAuthValid(String user, String password) {

        return (!StringUtils.isNotEmpty(user) || !StringUtils.isEmpty(password)) &&
                (!StringUtils.isNotEmpty(password) || !StringUtils.isEmpty(user));
    }
}
