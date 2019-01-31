package com.elastic.support.scrub;

import com.elastic.support.diagnostics.Diagnostic;
import com.elastic.support.diagnostics.DiagnosticInputs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.CallableStatement;


public class ScrubApp {

    private static Logger logger = LogManager.getLogger();

    public static void main(String[] args) {

        ScrubInputs scrubInputs = new ScrubInputs();
        scrubInputs.parseInputs(args);
        if(!scrubInputs.validate()){
            logger.info("Exiting...");
            System.exit(0);
        }

        try(Scrubber scrub = new Scrubber(scrubInputs);){
            scrub.exec();
        }
        catch (Exception e){
            logger.error("Error occurred - exiting:", e);
        }
    }
}