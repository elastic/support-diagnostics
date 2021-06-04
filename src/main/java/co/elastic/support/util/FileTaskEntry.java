/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class FileTaskEntry implements TaskEntry {
    private File file;
    private String rootDir;
    private static final Logger logger = LogManager.getLogger(FileTaskEntry.class);

    public FileTaskEntry(File file, String rootDir){
        this.rootDir = rootDir;
        this.file = file;
    }

    @Override
    public String content() {
        String contents = "";
        try {
            if(file.getName().endsWith(".gz")){
                GZIPInputStream gzi = new GZIPInputStream( new FileInputStream(file));
                return  IOUtils.toString(gzi,  "UTF-8");
            }

            return FileUtils.readFileToString(file, "UTF-8");
        } catch (IOException e) {
            logger.error("Error retrieving file: {}", file.getName(), e);
        }

        return file.getName() + ":error";
    }

    @Override
    public String entryName() {
        String name =  file.getAbsolutePath().replaceFirst(rootDir + SystemProperties.fileSeparator, "");
        return name.replaceFirst(".gz", "");
    }
}
