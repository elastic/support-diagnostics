package com.elastic.support.diagnostics.timed;

import com.beust.jcommander.Parameter;
import com.elastic.support.diagnostics.DiagnosticInputs;
import com.elastic.support.util.ResourceUtils;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class TimedExecutionDiagnosticInputs extends DiagnosticInputs {

    private static final Logger logger = LogManager.getLogger(TimedExecutionDiagnosticInputs.class);

    @Parameter(names = {"-executions"}, description = "Total number of diagnostic runs to execute. Defaults to 6")
    public int executions = 6;
    private static String executionsMsg = "Execution count must be greater than 1.";

    @Parameter(names = {"-interval"}, description = "Interval between executions in seconds. Defaults to 10 seconds.")
    public int interval = 10;
    private static String intervalMsg = "Interval must be at least 5 seconds.";

    public TimedExecutionDiagnosticInputs(String delimiter){
        super(delimiter);
    }

    public void runInteractive(){
        ResourceUtils.textIO.getTextTerminal().rawPrint("Timed execution mode. Enter the number of diagnostic executions to run and the delay between each run.");

        executions = ResourceUtils.textIO.newIntInputReader()
                .withDefaultValue(executions)
                .withMinVal(2)
                .read(SystemProperties.lineSeparator + executionsMsg);

        interval = ResourceUtils.textIO.newIntInputReader()
                .withDefaultValue(interval)
                .withMinVal(1)
                .read(SystemProperties.lineSeparator + intervalMsg);

        super.runInteractive();

        this.retryFailed = false;
        ResourceUtils.textIO.dispose();


    }

    public List<String> parseInputs(String[] args){
        List<String> errors = super.parseInputs(args);

        if(interval < 5){
            errors.add(intervalMsg);
        }
        if(executions < 2 ){
            errors.add(executionsMsg);
        }

        this.retryFailed = false;
        ResourceUtils.textIO.dispose();

        return errors;

    }



}
