package com.elastic.support.scrub;

import com.elastic.support.BaseService;
import com.elastic.support.Constants;
import com.elastic.support.diagnostics.commands.GenerateManifest;
import com.elastic.support.util.*;
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


public class ScrubService implements BaseService {

    private Logger logger = LogManager.getLogger(ScrubService.class);

    ScrubInputs inputs;
    public ScrubService(ScrubInputs inputs){
        this.inputs = inputs;
    }

    public void exec() {

        try {
            logger.info(Constants.CONSOLE, "Using version: {} of diagnostic-utility", SystemUtils.getToolVersion());

            SystemUtils.refreshDir(inputs.tempDir);

            ResourceUtils.executorService = Executors.newFixedThreadPool(inputs.workers);
            logger.info(Constants.CONSOLE, "Threadpool configured with {} workers.", inputs.workers);

            // Get a collection of entries to send parcel out to the task collection
            Vector<TaskEntry> entriesToScrub;
            String nodeString = "";
            switch (inputs.type) {
                case "tar":
                case "tar.gz":
                    String extractTarget = inputs.outputDir + SystemProperties.fileSeparator + "extract";
                    ArchiveUtils.extractArchive(inputs.scrub, extractTarget);
                    entriesToScrub = collectDirEntries(inputs.scrub, inputs.tempDir);
                    nodeString = getNodeInfoFromDir(extractTarget);
                    break;
                case "zip":
                    entriesToScrub = collectZipEntries(inputs.scrub, inputs.tempDir);
                    nodeString = getNodeInfoFromZip(inputs.scrub);
                    break;
                case "dir":
                    entriesToScrub = collectDirEntries(inputs.scrub, inputs.tempDir);
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
                tasks.add(new ScrubTask(processor, entry, inputs.tempDir));
            }

            List<Future<String>> futures = ResourceUtils.executorService.invokeAll(tasks);
            futures.forEach ( e -> {
                try {
                    logger.debug("processed: " + e.get());
                } catch (Exception ex) {
                    logger.error(e);
                }
            });

        } catch (Throwable t) {
            logger.error("Error occurred: ", t);
            logger.error(Constants.CONSOLE, "Issue encountered during scrub processing. {}.", Constants.CHECK_LOG);
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