package com.elastic.support.diagnostics;

import com.beust.jcommander.JCommander;
import com.elastic.support.diagnostics.chain.GlobalContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseInputs {

    protected JCommander jCommander;
    protected Logger logger = LogManager.getLogger(BaseInputs.class);


    public boolean validate(){
        return true;
    }

    public void parseInputs(String[] args){
        logger.info("Processing diagnosticInputs...");
        JCommander jc = new JCommander(this);
        jc.setCaseSensitiveOptions(true);
        jc.parse(args);
    }
}
