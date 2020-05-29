package com.elastic.support.util;

import com.elastic.support.Constants;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.zip.GZIPInputStream;

public class ZipFileTaskEntry implements TaskEntry {

    private static final Logger logger = LogManager.getLogger(ZipFileTaskEntry.class);
    private ZipFile zipFile;
    private ZipArchiveEntry zipEntry;
    private String archiveName;

    public ZipFileTaskEntry(ZipFile zipFile, ZipArchiveEntry zipEntry, String archiveName){
        this.zipEntry = zipEntry;
        this .zipFile = zipFile;
        this.archiveName = archiveName;
    }

    @Override
    public String content() {

        String contents = "";
        try {
            if(zipEntry.getName().endsWith(".gz")){
                GZIPInputStream gzi = new GZIPInputStream(zipFile.getInputStream(zipEntry));
                contents = IOUtils.toString(gzi,  "UTF-8");
            }
            else {
                contents = IOUtils.toString(zipFile.getInputStream(zipEntry), "UTF-8");
            }

        } catch (Exception e) {
            logger.error(Constants.CONSOLE, "Could not extract entry {}", zipEntry.getName());
        }

        return contents;

    }

    @Override
    public String entryName() {
        String name = zipEntry.getName().replaceFirst(archiveName, "");
        return name.replace(".gz", "");
    }
}
