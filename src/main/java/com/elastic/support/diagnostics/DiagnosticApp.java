package com.elastic.support.diagnostics;

import com.beust.jcommander.JCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DiagnosticApp {

	private static final Logger logger = LoggerFactory.getLogger(DiagnosticApp.class);

	public static void main(String[] args) throws Exception{

        InputParams inputs = new InputParams();
        JCommander jc = new JCommander(inputs);
        jc.setCaseSensitiveOptions(true);

        try{
            // Parse the incoming arguments from the command line
            // Assuming we didn't get an exception, do a final check to validate
            // whether if a username is entered so is a passoword, or vice versa.
            jc.parse(args);
            if(! validateAuth(inputs.getUsername(), inputs.getPassword())){
                throw new RuntimeException("If authenticating both username and password are required.");
            }

        }
        catch(RuntimeException e){
            System.out.println("Error:" + e.getMessage());
            jc.usage();
            System.exit(-1);
        }

        if(inputs.isHelp()) {
            jc.usage();
            System.exit(1);
        }

        try {
            //DiagnosticService diags = new DiagnosticService();
            DiagnosticChain dc = new DiagnosticChain();
            DiagnosticContext ctx = new DiagnosticContext();
            ctx.setInputParams(inputs);

            int reps = inputs.getReps();
            long interval = inputs.getInterval() * 1000;

            for (int i = 1; i <= reps; i++) {
                //diags.run(inputs);
                dc.execute(ctx);
                System.out.println("Run " + i + " of " + reps + " completed.");
                if (reps > 1) {
                    if (i < reps) {
                        System.out.println("Next run will occur in " + inputs.getInterval() + " seconds.\n");
                        Thread.sleep(interval);
                    }
                }
            }
        }
        catch (RuntimeException re){
            System.out.println("An error occurred while retrieving statistics. " + re.getMessage());
        }
    }

    private static boolean validateAuth(String userName, String password) {
        return ! ( (userName != null && password == null) || (password != null && userName == null) );
    }

}
