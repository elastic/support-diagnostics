package com.elastic.support.config;

import com.beust.jcommander.Parameter;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LocalCollectionInputs  extends BaseInputs {

    private static Logger logger = LogManager.getLogger(LocalCollectionInputs.class);

    @Parameter(names = {"-?", "--help"}, description = "Help contents.", help = true)
    private boolean help;

    @Parameter(names = {"-o", "--outputDir"}, description = "Fully qualified path to output directory.")
    // If no output directory was specified default to the working directory
    private String outputDir = SystemProperties.userDir;

    @Parameter(names = {"-l", "--logDir"}, description = "Required field.  Full path to Elasticsearch log directory.")
    private String logDir = "";

    @Parameter(names = {"-p", "--pid"}, description = "Optional field.  Process id of ES process if a full set of system calls is desired.")
    private String pid = "";

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

    public String getLogDir() {
        return logDir;
    }

    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }
}
