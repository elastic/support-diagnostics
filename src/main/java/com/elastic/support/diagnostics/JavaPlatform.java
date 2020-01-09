package com.elastic.support.diagnostics;

import com.elastic.support.Constants;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaPlatform {

    private static final Logger logger = LogManager.getLogger(JavaPlatform.class);

    public String platform;
    private String javaHome = "not set";
    public String javaExecutable ="/bin/java";
    public String javac = "javac";

    public JavaPlatform(String osName){

        switch (osName){
            case Constants.linuxPlatform:
                this.platform = Constants.linuxPlatform;
                break;

            case Constants.winPlatform:
                this.platform = Constants.winPlatform;
                javaExecutable = "\\bin\\java.exe";
                break;

            case Constants.macPlatform:
                this.platform = Constants.macPlatform;
                break;

            default:
                // default it to Linux
                logger.error("Failed to detect operating system for: {}", osName);
                this.platform = Constants.linuxPlatform;
        }
    }

    public String extractJdkPath(String processList) {

        String line;
        try (BufferedReader br = new BufferedReader(new StringReader(processList))) {
            while ((line = br.readLine()) != null) {
                if (line.contains(javaExecutable) && line.contains("-Des.")) {
                    String javaHome =  extractJavaHome(line);
                }
                else {
                    continue;
                }
            }
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Error obtaining the path for the JDK", e);
        }

        // If we got this far we couldn't find a JDK
        logger.info("Could not locate the location of the java executable used to launch Elasticsearch");
        throw new DiagnosticException("JDK not found.");
    }

    public String extractJavaHome(String jdkProcessString){

        // After the preceding cols are stripped, truncate the output behind the path to the executable.
        int jpathIndex = jdkProcessString.indexOf(javaExecutable);
        javaHome = jdkProcessString.substring(0, jpathIndex);

        return javaHome;
    }

    public boolean isJdkPresent(String result){
        if(result.contains(javac)){
            return true;
        }
        return false;
    }

}
