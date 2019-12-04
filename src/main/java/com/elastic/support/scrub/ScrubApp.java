package com.elastic.support.scrub;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ScrubApp {

    private static Logger logger = LogManager.getLogger(ScrubApp.class);

    public static void main(String[] args) {

        ScrubInputs scrubInputs = new ScrubInputs();
        scrubInputs.parseInputs(args);
        if (!scrubInputs.validate()) {
            logger.info("Exiting...");
            System.exit(0);
        }
        new ScrubService().exec(scrubInputs);

    }

}