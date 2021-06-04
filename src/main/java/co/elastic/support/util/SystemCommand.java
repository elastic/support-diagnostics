/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.util;


import java.io.Closeable;
import java.util.List;

public abstract class SystemCommand implements Closeable {

    public String osName;
    public abstract String runCommand(String command);
    public abstract void copyLogs(List<String> entries, String logDir, String targetDir);
    public abstract void copyLogsFromJournalctl(String serviceName, String targetDir);

}
