package com.elastic.support;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

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


    public boolean validate() {

        // If we're in help just shut down.
        if (isHelp()) {
            this.jCommander.usage();
            return false;
        }
        return true;
    }

    public String toString(){
        return "Output Directory: " + outputDir + SystemProperties.lineSeparator;
    }

}
