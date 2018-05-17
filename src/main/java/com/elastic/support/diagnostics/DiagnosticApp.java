package com.elastic.support.diagnostics;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class DiagnosticApp {

   private static final Logger logger = LogManager.getLogger();

   public static void main(String[] args) throws Exception {

      try {
         Diagnostic diag = new Diagnostic(args);
         if (diag.isProceedToRun()) {
            diag.exec();
         }
      } catch (Exception e) {
         logger.error(e.getMessage());
      }
   }
}
