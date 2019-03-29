package com.elastic.support.config;

import com.beust.jcommander.Parameter;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiagnosticInputs extends BaseInputs {

    private static Logger logger = LogManager.getLogger(DiagnosticInputs.class);

    @Parameter(names = {"-?", "--help"}, description = "Help contents.", help = true)
    private boolean help;
    @Parameter(names = {"-o", "--out", "--output", "--outputDir"}, description = "Fully qualified path to output directory.")
    // If no output directory was specified default to the working directory
    private String outputDir = SystemProperties.userDir;
    @Parameter(names = {"-h", "--host"}, description = "Required field.  Hostname, IP Address, or localhost.  HTTP access must be enabled.")
    private String host = "";
    @Parameter(names = {"--port"}, description = "HTTP or HTTPS listening port. Defaults to 9200.")
    private int port = 9200;
    @Parameter(names = {"-u", "--user"}, description = "User")
    private String user;
    @Parameter(names = {"-p", "--password"}, description = "Password", password = true)
    private String password;
    @Parameter(names = {"-s", "--ssl"}, description = "Use SSL?  No value required, only the option.")
    private boolean isSsl = false;
    @Parameter(names = {"--type"}, description = "DiagnosticService type to run. Enter standard, remote, logstash. Default is standard. Using remote will suppress retrieval of logs, configuration and system command info.")
    private String diagType = "standard";
    @Parameter(names = {"--ptp"}, description = "Insecure plain text password - allows you to input the password as a plain text argument for scripts. WARNING: Exposes passwords in clear text. Inherently insecure.")
    private String plainTextPassword = "";
    @Parameter(names = {"--reps"}, description = "Number of times to execute the diagnostic. Use to create multiple runs at timed intervals.")
    private int reps = 1;
    @Parameter(names = {"--interval"}, description = "Timed interval in minutes at which to execute the diagnostic. Minimum interval is 10 minutes.")
    private int interval = 600000;
    @Parameter(names = {"--scrub"}, description = "Set to true to use the scrub.yml dictionary to scrub logs and config files.  See README for more info.")
    private boolean scrubFiles = false;
    @Parameter(names = {"--noVerify"}, description = "Use this option to bypass hostname verification for certificate. This is inherently unsafe and NOT recommended.")
    private boolean skipVerification = false;
    @Parameter(names = {"--keystore"}, description = "Keystore for client certificate.")
    private String pkiKeystore;
    @Parameter(names = {"--keystorePass"}, description = "Keystore password for client certificate.", password = true)
    private String pkiKeystorePass;
    @Parameter(names = {"--accessLogs"}, description = "Use this option to collect access logs as well.")
    private boolean accessLogs = false;
    @Parameter(names = {"--bypassDiagVerify"}, description = "Bypass the diagnostic version check.")
    private boolean bypassDiagVerify = false;
    @Parameter (names = {"--proxyHost"}, description = "Proxy server hostname.")
    private String proxyHost;
    @Parameter (names = {"--proxyPort"}, description = "Proxy server port.")
    private int proxyPort = Constants.DEEFAULT_HTTP_PORT;
    @Parameter (names = {"--proxyUser"}, description = "Proxy server user name.")
    private String proxyUser;
    @Parameter (names = {"--proxyPassword"}, description = "Proxy server password.")
    private String proxyPassword;
    @Parameter (names = {"--dockerId"}, description = "ID of the docker container Elasticsearch is running in.")
    private String dockerId;

    public boolean isHelp() {
        return help;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        if (diagType.equalsIgnoreCase("logstash")) {
            if (this.port == 9200) {
                return Constants.LOGSTASH_PORT;
            }
        }
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

    public String getDiagType() {
        return diagType;
    }

    public void setDiagType(String diagType) {
        this.diagType = diagType;
    }

    public String getPlainTextPassword() {
        return plainTextPassword;
    }

    public void setPlainTextPassword(String plainTextPassword) {
        this.plainTextPassword = plainTextPassword;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public boolean isScrubFiles() {
        return scrubFiles;
    }

    public void setScrubFiles(boolean scrubFiles) {
        this.scrubFiles = scrubFiles;
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

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public boolean isAccessLogs() {
        return accessLogs;
    }

    public void setAccessLogs(boolean accessLogs) {
        this.accessLogs = accessLogs;
    }

    public boolean isBypassDiagVerify() {
        return bypassDiagVerify;
    }

    public void setBypassDiagVerify(boolean bypassDiagVerify) {
        this.bypassDiagVerify = bypassDiagVerify;
    }

    public boolean isSecured() {
        return (StringUtils.isNotEmpty(this.user) && StringUtils.isNotEmpty(this.password));
    }

    public String getDockerId() {
        return dockerId;
    }

    public void setDockerId(String dockerId) {
        this.dockerId = dockerId;
    }

    public String getTempDir() {

        if (this.diagType.equals(Constants.ES_DIAG_DEFAULT)) {
            return this.outputDir + SystemProperties.fileSeparator + Constants.ES_DIAG;

        } else {
            return this.outputDir + SystemProperties.fileSeparator + diagType + "-" + Constants.ES_DIAG;
        }
    }

    public String getScheme() {
        if (this.isSsl) {
            return "https";
        } else {
            return "http";
        }
    }

    public boolean isPki(){
        if(StringUtils.isEmpty(pkiKeystore) ){
            return false;
        }
        return true;
    }

    public boolean validate() {
        // If we're in help just shut down.
        if (isHelp()) {
            this.jCommander.usage();
            return false;
        }
        return (validateAuth() && validateIntervals());
    }

    public boolean validateAuth() {

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

    private boolean isAuthValid(String user, String password) {

        return (!StringUtils.isNotEmpty(user) || !StringUtils.isEmpty(password)) &&
                (!StringUtils.isNotEmpty(password) || !StringUtils.isEmpty(user));
    }

    public boolean validateIntervals() {

        boolean repsOK = true, intervalOK = true;

        if (this.getReps() > 1) {

            if (this.getReps() > 6) {
                logger.info("'--reps' specified [{}] exceed the maximum allowed [6].", this.getReps());
                logger.info("Use --help for allowed values.");
                repsOK = false;
            }

            if (this.getInterval() < 10) {
                logger.info("Interval specificed is lower than the minium allowed.");
                logger.info("Use --help for allowed values.");
                intervalOK = false;
            }
        }

        return (repsOK && intervalOK);

    }

    @Override
    public String toString() {
        return "DiagnosticInputs{" +
                "help=" + help +
                ", outputDir='" + outputDir + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", isSsl=" + isSsl +
                ", diagType='" + diagType + '\'' +
                ", skipVerification=" + skipVerification +
                ", skipAccessLogs=" + accessLogs +
                '}';
    }
}
