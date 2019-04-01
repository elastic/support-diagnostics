package com.elastic.support.scrub;
import com.elastic.support.config.ScrubInputs;
import com.elastic.support.BaseService;
import com.elastic.support.util.ArchiveUtils;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

public class ScrubService extends BaseService {

   private ScrubInputs inputs;

   public ScrubService(ScrubInputs inputs){
      this.inputs = inputs;
   }

   public void exec(){
      try {
         String archivePath = inputs.getArchive();
         String targetDir = inputs.getTargetDir();

         int pos = archivePath.lastIndexOf(SystemProperties.fileSeparator);

         if(targetDir == null){
            targetDir = archivePath.substring(0, pos) + SystemProperties.fileSeparator + "scrubbed";
         }
         else{
            targetDir = targetDir +  SystemProperties.fileSeparator + "scrubbed";
         }

         createFileAppender(targetDir, "scrubber.log");
         String scrubbedName = (archivePath.substring(pos + 1)).replace(".tar.gz", "");

         ArchiveUtils archiveUtils = new ArchiveUtils(new ScrubProcessor(inputs.getScrubFile()));
         archiveUtils.extractDiagnosticArchive(archivePath, targetDir );
         closeLogs();
         archiveUtils.createArchive(targetDir, scrubbedName);
         SystemUtils.nukeDirectory(targetDir);

         File tmp = new File(targetDir);
         FileUtils.deleteDirectory(tmp);
         logger.info("Deleted temp directory: {}.", targetDir);
      } catch (IOException ioe) {
         logger.error("Access issue with temp directory", ioe);
         throw new RuntimeException("Issue with creating temp directory - see logs for details.");
      }
      catch(Exception e){
         logger.error("Error extracting diagnostic archive", e);
      }

   }

}