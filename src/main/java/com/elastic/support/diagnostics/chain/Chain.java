package com.elastic.support.diagnostics.chain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class Chain implements Command {

    Logger logger = LoggerFactory.getLogger(Chain.class);
    List<Command> commandChain = new ArrayList<>();

    public Chain(List<String> commandList){
        try {
            for (String commandClass : commandList){
                Class clazz = Class.forName(commandClass);
                Command cmd = (Command)clazz.getConstructor().newInstance();
                commandChain.add(cmd);
            }
        } catch(Exception e) {
            String msg = "Error: could not create the configured command class.";
            logger.error(msg, e);
            throw new IllegalArgumentException("Error creating chain", e);
        }
    }

    public void execute(DiagnosticContext context){

        //Set up initial generic chain
        for(Command dc : commandChain){
            dc.execute(context);
        }
    }

}
