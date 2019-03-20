package com.elastic.support.config;

import com.beust.jcommander.Parameter;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ScrubInputs extends BaseInputs {

    private static Logger logger = LogManager.getLogger(ScrubInputs.class);

    @Parameter(names = {"-a", "--archive",}, required = true, description = "Required field.  Full path to the archive file to be scrubbed.")
    private String archive;
    @Parameter(names = {"-s", "--scrubFile"}, required = false, description = "Optional field.  Full path to the file where string tokens you wish to have removed resides.")
    private String scrubFile = "";
    @Parameter(names = {"-t", "--target",}, required = false, description = "Optional field.  Full path to the directory where the scrubbed archive will be written.")
    private String targetDir;
    @Parameter(names = {"-?", "--help"}, description = "Help contents.", help = true)
    private boolean help;

    public String getArchive() {
        return archive;
    }

    public void setArchive(String archive) {
        this.archive = archive;
    }

    public String getScrubFile() {
        return scrubFile;
    }

    public void setScrubFile(String scrubFile) {
        this.scrubFile = scrubFile;
    }

    public String getTargetDir() {
        return targetDir;
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    public boolean isHelp() {
        return help;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

}