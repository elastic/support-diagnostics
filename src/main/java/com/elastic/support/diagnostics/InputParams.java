package com.elastic.support.diagnostics;

import com.beust.jcommander.*;

public class InputParams {

    @Parameter(names = { "-h", "--?", "--help" }, help=true)
    private boolean help;

    @Parameter(names = {"--host", }, description = "Required field.  Hostname, IP Address, or localhost.  HTTP access must be enabled.")
    private String host="";

    @Parameter(names = { "--port" }, description = "HTTP or HTTPS listening port. Defaults to 9200")
    private int port = 9200;

    @Parameter(names = { "-u", "--user" }, description = "Username")
    private String username;

    @Parameter(names = { "-p", "--password", "--pwd" }, description = "Prompt for a password?  No password value required, only the option. Hidden from the command line on entry.", password = true)
    private String password;

    @Parameter(names = { "-o", "--out", "--output", "--outputDir" }, description = "Fully qualified path to output directory or c for current working directory.")
    private String outputDir = "cwd";

    @Parameter(names = { "-s", "--ssl", "--https"}, description = "Use SSL?  No value required, only the option.")
    private boolean isSsl = false;

    @Parameter(names={"-r", "--reps"}, description = "Number of times to execute the diagnostic. Use to create multiple runs at timed intervals.")
    private  int reps = 1;

    @Parameter(names={"-i", "--interval"}, description = "Elapsed time in seconds between diangostic runs when in repeating mode.  Minimum value is 30.")
    private long interval = 30;

    @Parameter(names={ "--archivedLogs"}, description = "Get archived logs in addition to current ones if present - No value required, only the option.")
    private boolean archivedLogs;

    @Parameter(names={ "--interactive"}, description = "Get archived logs in addition to current ones if present - No value required, only the option.")
    private boolean interactive;

    public boolean isInteractive() {
        return interactive;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

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

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        if (reps < 1) { throw new IllegalArgumentException("Number of repetitions must be at least 1.");}
        this.reps = reps;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        if (interval < 30) { throw new IllegalArgumentException("Delay interval must be at least 30.");}

        this.interval = interval;
    }

    public boolean isArchivedLogs() {
        return archivedLogs;
    }

    public void setArchivedLogs(boolean archivedLogs) {
        this.archivedLogs = archivedLogs;
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
