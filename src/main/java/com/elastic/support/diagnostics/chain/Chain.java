package com.elastic.support.diagnostics.chain;


import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;


public class Chain implements Command {

    Logger logger = LogManager.getLogger(Chain.class);
    List<Command> commandChain = new ArrayList<>();

    public Chain(List<String> commandList) {
        try {
            for (String commandClass : commandList) {
                Class clazz = Class.forName(commandClass);
                Command cmd = (Command) clazz.getConstructor().newInstance();
                commandChain.add(cmd);
            }
        } catch (Exception e) {
            String msg = "Error: could not create the configured command class.";
            logger.error(msg, e);
            throw new IllegalArgumentException("Error creating chain", e);
        }
    }

    public void execute(DiagnosticContext context) {

        try {
            //Set up initial generic chain
            for (Command dc : commandChain) {
                dc.execute(context);
            }
        } catch (DiagnosticException de) {
            // This was thrown to break out of the command chain so
            // just pass it along.
            throw de;
        } catch (Throwable t) {
            logger.log(SystemProperties.DIAG, "Unanticipated error.", t);
            throw new DiagnosticException("Uncaught error");
        }
    }

}
