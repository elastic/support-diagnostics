package com.elastic.support.diagnostics;

import com.beust.jcommander.JCommander;
import com.elastic.support.diagnostics.chain.DiagnosticChainExec;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DiagnosticApp {

   private static final Logger logger = LoggerFactory.getLogger(DiagnosticApp.class);

   public static void main(String[] args) throws Exception {

      InputParams inputs = new InputParams();
      JCommander jc = new JCommander(inputs);
      jc.setCaseSensitiveOptions(true);

      try {
         // Parse the incoming arguments from the command line
         // Assuming we didn't get an exception, do a final check to validate
         // whether if a username is entered so is a passoword, or vice versa.
         jc.parse(args);
         if (!validateAuth(inputs)) {
            throw new RuntimeException("If authenticating both username and password are required.");
         }

      } catch (RuntimeException e) {
         System.out.println("Error:" + e.getMessage());
         jc.usage();
         System.exit(-1);
      }

      if (inputs.isHelp()) {
         jc.usage();
         System.exit(1);
      }

      try {
         DiagnosticChainExec dc = new DiagnosticChainExec();
         DiagnosticContext ctx = new DiagnosticContext();
         ctx.setInputParams(inputs);

         int reps = inputs.getReps();
         long interval = inputs.getInterval() * 1000;

         if (reps > 1) {
            for (int i = 1; i <= reps; i++) {
               ctx.setCurrentRep(i);
               if (inputs.getDiagType().equalsIgnoreCase(Constants.STANDARD_DIAG) && i < (reps)) {
                  inputs.setSkipLogs(true);
               }
               else{
                  inputs.setSkipLogs(false);
               }
               dc.runDiagnostic(ctx);
               System.out.println("Run " + i + " of " + reps + " completed.");
               if (i < reps) {
                  System.out.println("Next run will occur in " + inputs.getInterval() + " seconds.\n");
                  Thread.sleep(interval);
               }

            }
         }
         else {
            dc.runDiagnostic(ctx);
         }

      } catch (RuntimeException re) {
         logger.error("Execution Error", re);
      }
   }

   private static boolean validateAuth(InputParams inputs) {

      String ptPassword = inputs.getPlainTextPassword();
      String userName = inputs.getUsername();
      String password = inputs.getPassword();
      if (!"".equals(ptPassword)) {
         password = ptPassword;
         inputs.setPassword(ptPassword);
      }

      return !((userName != null && password == null) || (password != null && userName == null));
   }

}
