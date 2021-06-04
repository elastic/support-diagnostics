/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.scrub;

import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.util.ArchiveUtils;
import co.elastic.support.util.FileTaskEntry;
import co.elastic.support.util.SystemProperties;
import co.elastic.support.util.SystemUtils;
import co.elastic.support.util.TaskEntry;
import co.elastic.support.util.ZipFileTaskEntry;
import co.elastic.support.BaseService;
import co.elastic.support.Constants;
import co.elastic.support.util.ArchiveUtils.ArchiveType;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScrubService extends BaseService {

    private Logger logger = LogManager.getLogger(ScrubService.class);

    public File exec(ScrubInputs inputs) throws DiagnosticException {
        ExecutorService executorService = null;
        String scrubDir = "";

        try {
            scrubDir = inputs.outputDir + SystemProperties.fileSeparator + "scrubbed-" + inputs.scrubbedFileBaseName;

            SystemUtils.refreshDir(scrubDir);

            // Redirect the log file output to the scrubbed output target location.
            createFileAppender(inputs.outputDir, "scrubber.log");
            executorService = Executors.newFixedThreadPool(inputs.workers);
            logger.info(Constants.CONSOLE, "Threadpool configured with {} workers.", inputs.workers);

            // Get a collection of entries to send parcel out to the task collection
            Vector<TaskEntry> entriesToScrub;
            String nodeString = "";
            switch (inputs.type) {
                case "tar":
                case "tar.gz":
                    String extractTarget = inputs.outputDir + SystemProperties.fileSeparator + "extract";
                    ArchiveUtils.extractArchive(inputs.scrub, extractTarget);
                    entriesToScrub = collectDirEntries(inputs.scrub, scrubDir);
                    nodeString = getNodeInfoFromDir(extractTarget);
                    break;
                case "zip":
                    entriesToScrub = collectZipEntries(inputs.scrub, scrubDir);
                    nodeString = getNodeInfoFromZip(inputs.scrub);
                    break;
                case "dir":
                    entriesToScrub = collectDirEntries(inputs.scrub, scrubDir);
                    nodeString = getNodeInfoFromDir(inputs.scrub);
                    break;
                default:
                    String rootDir = inputs.scrub.substring(0, inputs.scrub.lastIndexOf(SystemProperties.fileSeparator));
                    entriesToScrub = collectDirEntries(inputs.scrub, rootDir);
            }

            ScrubProcessor processor;
            if(StringUtils.isNotEmpty(nodeString)){
                processor = new ScrubProcessor(nodeString);
            }
            else {
                processor = new ScrubProcessor();
            }

            ArrayList<ScrubTask> tasks = new ArrayList<>();
            for(TaskEntry entry: entriesToScrub ){
                tasks.add(new ScrubTask(processor, entry, scrubDir));
            }

            List<Future<String>> futures = executorService.invokeAll(tasks);
            futures.forEach ( e -> {
                try {
                    logger.debug("processed: " + e.get());
                } catch (Exception ex) {
                    logger.error(e);
                }
            });

            // Finish up by zipping it.
            return createArchive(scrubDir, ArchiveType.fromString(inputs.archiveType));
        } catch (Throwable throwable) {
            throw new DiagnosticException("Could not scrub archive", throwable);
        } finally {
            executorService.shutdown();
            closeLogs();
            SystemUtils.nukeDirectory(scrubDir);
        }
    }

    public Vector<TaskEntry> collectDirEntries(String filename, String scrubDir) {

        Vector<TaskEntry> entries = new Vector<>();
        File file = new File(filename);
        String path = file.getAbsolutePath();

        if (!file.isDirectory()) {
            entries.add(new FileTaskEntry(file, scrubDir));
        } else {
            FileUtils.listFiles(file, null, true)
                    .forEach(f -> {
                        TaskEntry te = new FileTaskEntry(f, path);
                        if(f.isDirectory()){
                            new File(scrubDir + SystemProperties.fileSeparator + te.entryName()).mkdir();
                        }
                        else{
                            entries.add(te);
                        }
                    });
        }

        return entries;
    }

    public Vector<TaskEntry> collectZipEntries(String filename, String scrubDir) {
        Vector<TaskEntry> archiveEntries = new Vector<>();
        try {
            ZipFile zf = new ZipFile(new File(filename));
            Enumeration<ZipArchiveEntry> entries = zf.getEntries();
            ZipArchiveEntry ent = entries.nextElement();
            String archiveName = ent.getName();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry zae = entries.nextElement();
                TaskEntry te = new ZipFileTaskEntry(zf, zae, archiveName);
                if(zae.isDirectory()){
                    new File(scrubDir + SystemProperties.fileSeparator + te.entryName()).mkdir();
                }
                else{
                    archiveEntries.add(te);
                }
            }
        } catch (IOException e) {
            logger.error(Constants.CONSOLE, "Error obtaining zip file entries");
            logger.error(e);
        }

        return archiveEntries;

    }

    private String getNodeInfoFromZip(String zipFile) {

        try {
            ZipFile zf = new ZipFile(new File(zipFile));
            String rootPath = zf.getEntriesInPhysicalOrder().nextElement().getName();
            ZipArchiveEntry nodeEntry = zf.getEntry(rootPath + "nodes.json");
            return IOUtils.toString(zf.getInputStream(nodeEntry), "UTF-8");
        } catch (IOException e) {
            logger.error(Constants.CONSOLE, "Couldn't retrieve node artifacts for default scrub");
            logger.error(e);
        }
        return "";
    }

    private String getNodeInfoFromDir(String dir) {

        try {
            File target = new File(dir + SystemProperties.fileSeparator + "nodes.json");
            if(target.exists()){
                return FileUtils.readFileToString(target, "UTF-8");
            }
        } catch (IOException e) {
            logger.error(Constants.CONSOLE, "Couldn't retrieve node artifacts for default scrub");
            logger.error(e);
        }
        return "";
    }

}