package com.elastic.support;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.elastic.support.util.SystemProperties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.beryx.textio.StringInputReader;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseInputs {

    private static final Logger logger = LogManager.getLogger(BaseInputs.class);

    public static final TextIO textIO = TextIoFactory.getTextIO();
    public static final String outputDirDescription = "Fully qualified path to an output directory. This directory must already exist. If not specified the diagnostic directory will be used.";
    public static final String interactiveModeDescription = "Interactive mode. Prompt for all values and validate as you go.";
    public static final String bypassDiagVerifyDescription = "Bypass the diagnostic version check. Use when internet outbound HTTP access is blocked by a firewall.";
    protected List<String> emptyList = new ArrayList<>();
    protected JCommander jCommander;

    // Input Fields

    @Parameter(names = {"-?", "--help"}, description = "Help contents.", help = true)
    public boolean help;

    // If no output directory was specified default to the working directory
    @Parameter(names = {"-o", "--out", "--output", "--outputDir"}, description = outputDirDescription)
    public String outputDir = SystemProperties.userDir;

    @Parameter(names = {"-i", "--interactive"}, description = interactiveModeDescription)
    public boolean interactive = false;

    // Stop the diag from checking itself for latest version.
    @Parameter(names = {"--bypassDiagVerify"}, description = bypassDiagVerifyDescription)
    public boolean bypassDiagVerify = false;

    // Input Fields

    // Input Readers

    protected StringInputReader outputDirectoryReader = textIO.newStringInputReader()
            .withValueChecker(( String val, String propname) -> validateOutputDir(val));

    // Input Readers

    public abstract boolean runInteractive();

    public void parseInputs(String[] args){
        logger.info("Processing diagnosticInputs...");
        jCommander = new JCommander(this);
        jCommander.setCaseSensitiveOptions(true);
        jCommander.parse(args);
    }

    public void usage(){
        jCommander.usage();
    }

    public List<String> validate() {
        // If we're in help just shut down.
        if (help) {
            jCommander.usage();
        }
        return emptyList;
    }

    public List<String> validateOutputDir(String outputDir){
        File outFile = new File(outputDir);
        if(outFile.exists() && outFile.isDirectory()){
            return null;
        }
        else{
            return Collections.singletonList("Output Directory does not exist");
        }
    }

    public String toString(){
       return "outputDir: " + outputDir + ",";
    }

}
