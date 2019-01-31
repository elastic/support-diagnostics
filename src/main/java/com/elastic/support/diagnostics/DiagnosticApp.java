package com.elastic.support.diagnostics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiagnosticApp {

   private static final Logger logger = LogManager.getLogger();

   public static void main(String[] args){

       DiagnosticInputs diagnosticInputs = new DiagnosticInputs();
       diagnosticInputs.parseInputs(args);
       if(!diagnosticInputs.validate()){
           logger.info("Exiting...");
           System.exit(0);
       }

       try(Diagnostic diag = new Diagnostic();){
           diag.run();
       }
       catch (Exception e){
           logger.error("Error occurred - exiting:", e);
       }

   }
}
