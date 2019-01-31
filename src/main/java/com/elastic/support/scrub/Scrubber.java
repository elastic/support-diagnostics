package com.elastic.support.diagnostics.scrub;
import com.elastic.support.diagnostics.AbstractDiagnostic;
import com.elastic.support.util.ArchiveUtils;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class Scrubber extends AbstractDiagnostic {

   private ScrubInputParams inputs = new ScrubInputParams();

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

         SystemUtils.createFileAppender(targetDir, "scrubber.log");
         String scrubbedName = (archivePath.substring(pos + 1)).replace(".tar.gz", "");
         ArchiveUtils archiveUtils = new ArchiveUtils(new ScrubberUtils(inputs.getScrubFile()));
         archiveUtils.extractDiagnosticArchive(archivePath, targetDir );
         SystemUtils.closeLogs();
         archiveUtils.createArchive(targetDir, scrubbedName);
         SystemUtils.nukeTempDir(targetDir);

         File tmp = new File(targetDir);
         //tmp.setWritable(true, false);
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