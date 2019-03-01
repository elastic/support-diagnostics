package com.elastic.support.scrub;

import com.elastic.support.config.ScrubInputs;
import com.elastic.support.util.JsonYamlUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;


public class ScrubApp {

    private static Logger logger = LogManager.getLogger(ScrubApp.class);

    public static void main(String[] args) {

        ScrubInputs scrubInputs = new ScrubInputs();
        scrubInputs.parseInputs(args);
        if(!scrubInputs.validate()){
            logger.info("Exiting...");
            System.exit(0);
        }

        try(ScrubService scrub = new ScrubService(scrubInputs);){
            scrub.exec();
        }
        catch (Exception e){
            logger.error("Error occurred - exiting:", e);
        }
    }

}