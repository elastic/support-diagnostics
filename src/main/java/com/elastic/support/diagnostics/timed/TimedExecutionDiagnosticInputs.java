package com.elastic.support.diagnostics.timed;

import com.beust.jcommander.Parameter;
import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagnosticInputs;
import com.elastic.support.util.ResourceCache;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class TimedExecutionDiagnosticInputs extends DiagnosticInputs {

    private static final Logger logger = LogManager.getLogger(TimedExecutionDiagnosticInputs.class);

    @Parameter(names = {"--executions", "-e"}, description = "Total number of diagnostic runs to execute. Defaults to 6")
    public int executions = 6;
    private static String executionsMsg = "Execution count must be greater than 1.";

    @Parameter(names = {"--interval", "-i"}, description = "Interval between executions in seconds. Defaults to 10 seconds.")
    public int interval = 10;
    private static String intervalMsg = "Interval must be at least 20 seconds.";


    public void runInteractive(){
        ResourceCache.textIO.getTextTerminal().rawPrint("Timed execution mode. Enter the number of diagnostic executions to run and the delay between each run.");

        executions = ResourceCache.textIO.newIntInputReader()
                .withDefaultValue(executions)
                .withMinVal(2)
                .read(SystemProperties.lineSeparator + executionsMsg);

        interval = ResourceCache.textIO.newIntInputReader()
                .withDefaultValue(interval)
                .withMinVal(1)
                .read(SystemProperties.lineSeparator + intervalMsg);

        super.runInteractive();

    }

    public List<String> parseInputs(String[] args){
        List<String> errors = super.parseInputs(args);

        if(interval < 5){
            errors.add(intervalMsg);
        }
        if(executions < 2 ){
            errors.add(executionsMsg);
        }

        return errors;

    }



}
