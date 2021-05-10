package com.elastic.support.diagnostics;

import com.elastic.support.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
                logger.info(Constants.CONSOLE, "Failed to detect operating system for: {}", osName);
                this.platform = Constants.linuxPlatform;
        }
    }

    public String extractJavaHome(String jdkProcessString){
        // After the preceding cols are stripped, truncate the output behind the path to the executable.
        int jpathIndex = jdkProcessString.indexOf(javaExecutable);
        javaHome = jdkProcessString.substring(0, jpathIndex);

        return javaHome;
    }

}