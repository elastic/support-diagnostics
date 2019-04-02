package com.elastic.support.config;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseInputs {

    protected JCommander jCommander;
    private static final Logger logger = LogManager.getLogger(BaseInputs.class);

    public void parseInputs(String[] args){
        logger.info("Processing diagnosticInputs...");
        JCommander jc = new JCommander(this);
        jc.setCaseSensitiveOptions(true);
        jc.parse(args);
    }
}
