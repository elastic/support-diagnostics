package com.elastic.support;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseInputs {

    private static final Logger logger = LogManager.getLogger(BaseInputs.class);
    public static final String YesNoValidationMessage = "Valid responses: Y, N or hit <Enter Key> for N.";
    protected final StringBuffer toStringBuffer = new StringBuffer();
    protected List<String> messages = new ArrayList<>();
    protected JCommander jCommander;
    protected Console console = System.console();
    protected boolean isIde = (console == null);

    public void parseInputs(String[] args){
        logger.info("Processing diagnosticInputs...");
        if(isIde){
            logger.info("System console not available - input prompts will be disabled.");
        }
        toStringBuffer.delete(0, toStringBuffer.length());;
        messages.clear();
        jCommander = new JCommander(this);
        jCommander.setCaseSensitiveOptions(true);
        jCommander.parse(args);
    }

    @Parameter(names = {"-?", "--help"}, description = "Help contents.", help = true)
    public boolean help;

    // If no output directory was specified default to the working directory
    @Parameter(names = {"-o", "--out", "--output", "--outputDir"}, description = "Fully qualified path to output directory.")
    public String outputDir = SystemProperties.userDir;


    @Parameter(names = {"-i", "--interactive"}, description = "Interactive mode. Steo through the inputs and prompt for values.")
    protected boolean interactive = false;

    public boolean validate() {

        // If we're in help just shut down.
        if (help) {
            this.jCommander.usage();
            return false;
        }
        return true;
    }

    // Null safe wrappers for the console input functions.
    protected String consoleInput(String message){
        return ObjectUtils.defaultIfNull(console.readLine(message),"");
    }
    protected String consolePassword(String message){
        char[] returnArray = console.readPassword(message);
        if(returnArray == null){
            return "";
        }
        return new String(returnArray);
    }
    public String promptOutputDir(){
        System.out.println("");
        System.out.println("Enter the full path to the directory where diagnostic output will be written.");
        System.out.println("If no value is provided the diagnostic directory will be used.");
        return console.readLine("Path to the output directory: ");
    }
    public boolean convertYesNoStringInput(String input){
        // Check for a valid input
        if(StringUtils.isEmpty(input)){
            throw new IllegalArgumentException();
        }
        input = input.trim().toLowerCase();

        switch (input){
            case "y" :
                return true;
            case "yes" :
                return true;
            case "n" :
                return false;
            case "no":
                return false;
            default:
                throw new IllegalArgumentException();
        }

    }

    public String toString(){
       return "outputDir: " + outputDir + ",";
    }

}
