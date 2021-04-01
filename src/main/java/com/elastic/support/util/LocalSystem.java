package com.elastic.support.util;

import com.elastic.support.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

import java.util.List;


public class LocalSystem extends SystemCommand {

    Logger logger = LogManager.getLogger(LocalSystem.class);
    private ProcessBuilder pb;
    private static final String[] wincCmds = {"cmd", "/c"};
    private static final String[] nixcCdms = {"/bin/sh", "-c"};
    private String encoding = Constants.UTF_8;

    public LocalSystem(String osName) {

        this.osName = osName;

        switch (osName) {
            case Constants.linuxPlatform:
            case Constants.macPlatform:
                pb = new ProcessBuilder(nixcCdms);
                break;
            case Constants.winPlatform:
                pb = new ProcessBuilder(wincCmds);
                // Windows hack - wmmic uses UTF-16
                encoding = Constants.UTF_16;
                break;
            default:
                pb = new ProcessBuilder(nixcCdms);
                logger.info("Unrecognized OS: {} - using Linux as default.", osName);
        }
        pb.redirectErrorStream(true);

    }

    public String runCommand(String cmd) {

        StringBuffer sb = new StringBuffer();

        try {
            List current = pb.command();
            if (current.size() == 2) {
                current.add(cmd);
            } else {
                current.set(2, cmd);
            }

            pb.redirectErrorStream(true);
            Process pr = pb.start();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line + SystemProperties.lineSeparator);
            }
            pr.waitFor();

        } catch (Exception e) {
            logger.info("Error encountered running {}", cmd);
            logger.error( "System command error", e);
        }

        return sb.toString();
    }


    @Override
    public void copyLogs(List<String> entries, String logDir, String targetDir) {

        for(String entry: entries){
            try {
                String source = logDir + SystemProperties.fileSeparator + entry;
                String target = targetDir + SystemProperties.fileSeparator + entry;
                FileUtils.copyFile(new File(source), new File(target));
            } catch (IOException e) {
                logger.info("Error retrieving log: {}. Bypassing.", entry);
                logger.error( e);
            }
        }
    }

   /**
    * On this function we will try to collect the journalctl logs, 
    * Some services as Kibana installed with the RPM package will give the access to the logs using the journalctl command
    *
    * @param  serviceName service name defined by RPM
    * @param  targetDir temporary path where the data need to be stored
    */
    @Override
    public void copyLogsFromJournalctl(String serviceName, String targetDir) {

        String tempDir = "templogs";
        String mkdir = "mkdir templogs";
        String journalctl = "journalctl -u {{SERVICE}} > '{{TEMP}}/{{SERVICE}}.log'";
        journalctl = journalctl.replace("{{SERVICE}}", serviceName);
        journalctl = journalctl.replace("{{TEMP}}", tempDir);

        try {
            runCommand(mkdir);
            runCommand(journalctl);
            String source = "{{TEMP}}/{{SERVICE}}.log";
            source = source.replace("{{SERVICE}}", serviceName);
            source = source.replace("{{TEMP}}", tempDir);
            String target = targetDir + SystemProperties.fileSeparator + serviceName;
            FileUtils.copyFile(new File(source), new File(target));
            // clean up the temp logs on the remote host
            runCommand("rm -Rf templogs");
        } catch (IOException e) {
            logger.info("Error retrieving log: {}. Bypassing.", serviceName);
            logger.error( e);
        }
    }


    @Override
    public void close() throws IOException {
        // Nothing to do for this one.
    }
}
