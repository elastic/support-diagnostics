package com.elastic.support.scrub;

import com.elastic.support.Constants;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.TaskEntry;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.concurrent.Callable;

public class ScrubTask implements Callable<String> {

    private static Logger logger = LogManager.getLogger(ScrubTask.class);
    ScrubProcessor processor;
    TaskEntry entry;
    String dir;

    public ScrubTask(ScrubProcessor processor, TaskEntry entry, String dir){
        this.entry = entry;
        this.processor = processor;
        this.dir = dir;
    }

    @Override
    public String call() throws Exception {

        String result = "";
        // If it's in remove we not only don't process it we don't write it to the scrubbed archive either
        if(processor.isRemove(entry.entryName())){
            logger.info(Constants.CONSOLE, "Removing entry: {}", entry.entryName());
            return entry.entryName() + ":removed";
        }

        String content = entry.content();

        if( processor.isExclude(entry.entryName())) {
            result = entry.entryName() + ":excluded";
            logger.info(Constants.CONSOLE, "Excluded from sanitization: {}", entry.entryName());

        }
        else{
            content = processor.processAutoscrub(content);
            content = processor.processLineWithTokens(content, entry.entryName());
            result = entry.entryName() + ":sanitized";
            logger.info(Constants.CONSOLE, "Processed entry: {}", entry.entryName());

        }

        String targetFileName = dir + SystemProperties.fileSeparator + entry.entryName();
        FileUtils.writeStringToFile(new File(targetFileName), content, "UTF-8");

        return result;
    }
}
