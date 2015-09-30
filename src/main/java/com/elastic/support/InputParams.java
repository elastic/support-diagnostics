package com.elastic.support;

import com.beust.jcommander.*;

public class InputParams {

    @Parameter(names = { "-h", "--?", "--help" }, help=true)
    private boolean help;

    @Parameter(names = {"--host", }, description = "Hostname, IP Address, or localhost if a node is present on this host that is part of the cluster and that has HTTP access enabled.")
    private String host="localhost";

    @Parameter(names = { "--port", "--listen" }, description = "HTTP or HTTPS listening port.")
    private int port = 9200;

    @Parameter(names = { "-u", "--user" }, description = "Username")
    private String username;

    @Parameter(names = { "-p", "--password", "--pwd" }, description = "Prompt for a password?  No password value required, only the option. Hidden from the command line on entry.", password = true)
    private String password;

    @Parameter(names = { "-o", "--out"," --output", "--outputDir" }, description = "Fully qualified path to output directory or c for current working directory.")
    private String outputDir = "cwd";

    @Parameter(names = { "-s", "--ssl", "--https"}, description = "Use SSL?  No value required, only the option.")
    private
    boolean isSsl = false;

    private boolean secured = false;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public boolean isSecured() {
        return (this.username != null && this.password != null);
    }

    public boolean isSsl() {
        return isSsl;
    }

    public void setIsSsl(boolean isSsl) {
        this.isSsl = isSsl;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    public boolean isHelp() {
        return help;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }


    public String getUrl(){
        String protocol;

        if(this.isSsl) {
            protocol = "https";
        }
        else{
            protocol = "http";
        }

        return protocol + "://" + host + ":" + port;
    }

    @Override
    public String toString() {
        return "InputParams{" +
                "help=" + help +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", outputDir='" + outputDir + '\'' +
                ", isSsl=" + isSsl +
                ", secured=" + secured +
                '}';
    }
}
