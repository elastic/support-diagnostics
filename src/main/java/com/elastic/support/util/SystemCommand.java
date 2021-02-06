package com.elastic.support.util;


import java.io.Closeable;
import java.util.List;

public abstract class SystemCommand implements Closeable {

    public String osName;
    public abstract String runCommand(String command);
    public abstract void copyLogs(List<String> entries, String logDir, String targetDir);
    public abstract void copyLogsFromJournalctl(String serviceName, String targetDir);

}
