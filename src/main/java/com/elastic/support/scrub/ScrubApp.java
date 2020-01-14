package com.elastic.support.scrub;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;


public class ScrubApp {

    private static Logger logger = LogManager.getLogger(ScrubApp.class);

    public static void main(String[] args) {

        ScrubInputs scrubInputs = new ScrubInputs();
        scrubInputs.parseInputs(args);
        List<String> errors = scrubInputs.validate();

        if (errors.size() >0) {
            for(String err: errors){
                logger.error(err);
            }
            logger.info("Exiting...");
            System.exit(0);
        }
        new ScrubService().exec(scrubInputs);

    }

}