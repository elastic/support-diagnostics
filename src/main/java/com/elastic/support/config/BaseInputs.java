package com.elastic.support.config;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseInputs {

    protected JCommander jCommander;
    private static final Logger logger = LogManager.getLogger(BaseInputs.class);

    public void parseInputs(String[] args){
        logger.info("Processing diagnosticInputs...");
        jCommander = new JCommander(this);
        jCommander.setCaseSensitiveOptions(true);
        jCommander.parse(args);
    }


    @Parameter(names = {"-?", "--help"}, description = "Help contents.", help = true)
    protected boolean help;
    public boolean isHelp() {
        return help;
    }
    public void setHelp(boolean help) {
        this.help = help;
    }

    @Parameter(names = {"-o", "--out", "--output", "--outputDir"}, description = "Fully qualified path to output directory.")
    // If no output directory was specified default to the working directory
    protected String outputDir = SystemProperties.userDir;
    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
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

    public boolean validate() {
        // If we're in help just shut down.
        if (isHelp()) {
            this.jCommander.usage();
            return false;
        }
        return true;
    }
}
