package com.elastic.support.scrub;

import com.beust.jcommander.Parameter;
import com.elastic.support.BaseInputs;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;


public class ScrubInputs extends BaseInputs {

    private static Logger logger = LogManager.getLogger(ScrubInputs.class);

    // Start Input Fields
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

    // End Input Fields

    public ScrubInputs(){
        super();
    }

    public boolean runInteractive(){
        logger.info("No interactive mode available at this time. Command line only.");
        logger.info("Please consult the documentation for instructions.");
        return true;
    }

    public List<String> parseIinputs(String[] args){
        List<String> errors = super.parseInputs(args);

        if (help) {
            this.jCommander.usage();
            return emptyList;
        }

        if(StringUtils.isEmpty(infile) && StringUtils.isEmpty(archive) ){
            errors.add("You must specify either an archive or individual file to process.");
        }

        if(StringUtils.isNotEmpty(infile) && StringUtils.isNotEmpty(archive) ){
            errors.add("You cannot specify both an archive and individual file to process.");
        }

        return errors;
    }

    @Override
    public String toString() {
        return "ScrubInputs{" +
                "archive='" + archive + '\'' +
                ", infile='" + infile + '\'' +
                ", configFile='" + configFile + '\'' +
                '}';
    }
}