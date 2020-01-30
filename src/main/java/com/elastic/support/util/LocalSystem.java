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
            logger.log(SystemProperties.DIAG, "System command error", e);
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
                logger.log(SystemProperties.DIAG, e);
            }
        }
    }


    @Override
    public void close() throws IOException {
        // Nothing to do for this one.
    }
}
