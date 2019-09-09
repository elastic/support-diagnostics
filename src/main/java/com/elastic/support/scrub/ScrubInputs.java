package com.elastic.support.scrub;

import com.beust.jcommander.Parameter;
import com.elastic.support.config.BaseInputs;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ScrubInputs extends BaseInputs {

    private static Logger logger = LogManager.getLogger(ScrubInputs.class);

    @Parameter(names = {"-a", "--archive"}, description = "Required field if infile not specified.  Full path to the archive file to be scrubbed.")
    private String archive;
    public String getArchive() {
        return archive;
    }
    public void setArchive(String archive) {
        this.archive = archive;
    }

    @Parameter(names = {"-i", "--infile"}, description = "Required field if archive not specified.  Full path to the individual file to be scrubbed.")
    private String infile;
    public String getInfile() {
        return infile;
    }
    public void setInfile(String infile) {
        this.infile = infile;
    }

    @Parameter(names = {"-c", "--config"}, required = false, description = "Optional field.  Full path to the .yml file where string tokens you wish to have removed resides.")
    private String configFile = "";
    public String getConfigFile() {
        return configFile;
    }
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }


    public boolean validate() {
        // If we're in help just shut down.
        if (isHelp()) {
            this.jCommander.usage();
            return false;
        }

        if(StringUtils.isEmpty(infile) && StringUtils.isEmpty(archive) ){
            logger.warn("You must specify either an archive or individual file to process.");
            return false;
        }

        if(StringUtils.isNotEmpty(infile) && StringUtils.isNotEmpty(archive) ){
            logger.warn("You cannot specify both an archive and individual file to process.");
            return false;
        }

        return true;

    }

    public String getTempDir() {
        return outputDir + SystemProperties.fileSeparator + "scrubbed";
    }
}