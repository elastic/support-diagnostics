package com.elastic.support.scrub;

import com.beust.jcommander.Parameter;
import com.elastic.support.BaseInputs;
import com.elastic.support.util.ResourceCache;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.ObjectUtils;
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
        String operation = standardStringReader
                .withNumberedPossibleValues("Scrub Archive", "Scrub Single File")
                .withIgnoreCase()
                .read(SystemProperties.lineSeparator + "Select the type of file you wish to scrub." );
        logger.info("");

        if(operation.toLowerCase().contains("archive")) {
            archive = ResourceCache.textIO.newStringInputReader()
                    .withInputTrimming(true)
                    .withValueChecker((String val, String propname) -> validateRequiredFile(val))
                    .read("Enter the full path of the archive you wish to import.");
        }
        else {
            infile = ResourceCache.textIO.newStringInputReader()
                    .withInputTrimming(true)
                    .withValueChecker((String val, String propname) -> validateRequiredFile(val))
                    .read("Enter the full path of the individual file you wish to import.");
        }

        logger.info("");

        logger.info("If you do not specify a yaml configuration file, the utility will automatically");
        logger.info("obfuscate IP and MAC addresses by default. You do NOT need to configure that functionality.");
        logger.info("If you wish to extend for additional masking you MUST explicitly enter a file to input.");
        logger.info("Note that for docker containers this must be a file within the configured volume.");
        configFile = ResourceCache.textIO.newStringInputReader()
                .withInputTrimming(true)
                .withMinLength(0)
                .withValueChecker((String val, String propname) -> validateFile(val))
                .read("Enter the full path of the Configuration file you wish to import or hit enter to take the default IP/MAC scrub.");

        if(runningInDocker){
            logger.info("Result will be written to the configured Docker volume.");
        }
        else{
            runOutputDirInteractive();
        }

        return true;
    }

    public List<String> parseIinputs(String[] args){
        List<String> errors = super.parseInputs(args);

        if(StringUtils.isEmpty(infile) && StringUtils.isEmpty(archive) ){
            errors.add("You must specify either an archive or individual file to process.");
        }

        if(StringUtils.isNotEmpty(infile) && StringUtils.isNotEmpty(archive) ){
            errors.add("You cannot specify both an archive and individual file to process.");
        }

        if(StringUtils.isNotEmpty(archive)){
            errors.addAll(ObjectUtils.defaultIfNull(validateRequiredFile(archive), emptyList));
        }

        if(StringUtils.isNotEmpty(infile)){
            errors.addAll(ObjectUtils.defaultIfNull(validateRequiredFile(infile), emptyList));
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
