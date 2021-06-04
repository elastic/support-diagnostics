/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.scrub;

import co.elastic.support.BaseInputs;
import co.elastic.support.util.ResourceCache;
import co.elastic.support.util.SystemProperties;
import com.beust.jcommander.Parameter;
import co.elastic.support.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.List;


public class ScrubInputs extends BaseInputs {

    private static Logger logger = LogManager.getLogger(ScrubInputs.class);

    // Start Input Fields
    @Parameter(names = {"-i", "--input"}, description = "Required field. Full path to the archive file, directory, or individual file to be scrubbed.")
    public String scrub;

    @Parameter(names = {"--workers"}, description = "Optional field. How many processing instances to run. Defaults to the number of detected cores.")
    public int workers = Runtime.getRuntime().availableProcessors();

    // End Input Fields

    public String type = "zip";
    public boolean isArchive = true;
    public String scrubbedFileBaseName;

    public boolean runInteractive() {

        scrub = ResourceCache.textIO.newStringInputReader()
                .withInputTrimming(true)
                .withValueChecker((String val, String propname) -> validateScrubInput(val))
                .read("Enter the full path of the archive you wish to import.");

        workers = ResourceCache.textIO.newIntInputReader()
                .withMinVal(0)
                .withDefaultValue(workers)
                .read("Enter the number of workers to run in parallel. Defaults to the detected number of processors: " + workers);

        logger.info(Constants.CONSOLE, "");
        logger.info(Constants.CONSOLE, "The utility will obfuscate IP and MAC addresses by default. You do NOT need to configure that functionality.");
        logger.info(Constants.CONSOLE, "If you wish to extend for additional masking you MUST explicitly enter a file to input.");
        logger.info(Constants.CONSOLE, "Note that for docker containers this must be a file within the configured volume.");

        if (runningInDocker) {
            logger.info(Constants.CONSOLE, "Result will be written to the configured Docker volume.");
        } else {
            runOutputDirInteractive();
        }

        return true;
    }

    public List<String> parseIinputs(String[] args) {
        List<String> errors = super.parseInputs(args);

        List valid = validateScrubInput(scrub);
        if(valid != null){
            errors.addAll(valid);
        }

        return errors;
    }

    public List<String> validateScrubInput(String val) {
        if (StringUtils.isEmpty(val.trim())) {
            return Collections.singletonList("Input archive, directory, or single file is required.");
        }

        File file = new File(val);

        if (!file.exists() ) {
            return Collections.singletonList("Specified required file location could not be located or is a directory.");
        }
        int filePosition = scrub.lastIndexOf(SystemProperties.fileSeparator);

        if( scrub.endsWith(".zip")){
            this.type = "zip";
            scrubbedFileBaseName = (scrub.substring(filePosition + 1)).replace(".zip", "");
        }
        else if(scrub.endsWith(".tar.gz")){
            this.type = "tar.gz";
            scrubbedFileBaseName = (scrub.substring(filePosition + 1)).replace(".tar.gz", "");
        }
        else if(scrub.endsWith(".tar")){
            this.type = "tar";
            scrubbedFileBaseName = (scrub.substring(filePosition + 1)).replace(".tar", "");
        }
        else if(file.isDirectory()){
            this.type = "dir";
            isArchive = false;
            scrubbedFileBaseName = scrub.substring(filePosition + 1);
        }
        else{
            this.type = "file";
            isArchive = false;
            scrubbedFileBaseName = scrub.substring(filePosition + 1, scrub.lastIndexOf("."));
        }

        return null;

    }

    @Override
    public String toString() {
        return "ScrubInputs{" +
                "input='" + scrub + '\'' +
                '}';
    }
}