package com.elastic.support.scrub;
import com.elastic.support.Constants;
import com.elastic.support.BaseService;
import com.elastic.support.util.ArchiveUtils;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class ScrubService extends BaseService {

   private ScrubInputs inputs;
   private Logger logger = LogManager.getLogger(ScrubService.class);


   public ScrubService(ScrubInputs inputs){
      this.inputs = inputs;
   }

   public void exec(){
      try {
         String filePath = inputs.getArchive();
         String infilePath = inputs.getInfile();
         String outputDir = inputs.getOutputDir();
         String temp = inputs.getTempDir();

         int pos;
         boolean isArchive = true;

         if(StringUtils.isNotEmpty(filePath)){
            pos = filePath.lastIndexOf(SystemProperties.fileSeparator);
         }
         else{
            isArchive = false;
            pos = infilePath.lastIndexOf(SystemProperties.fileSeparator);
            filePath = infilePath;
         }

         if(StringUtils.isEmpty(outputDir)  ){
            outputDir = filePath.substring(0, pos) + SystemProperties.fileSeparator;
         }

         // Start out clean
         SystemUtils.nukeDirectory(temp);
         createFileAppender(temp, "scrubber.log");

         if(isArchive){
            String scrubbedName = (filePath.substring(pos + 1)).replace(".tar.gz", "");
            ArchiveUtils archiveUtils = new ArchiveUtils(new ScrubProcessor(inputs.getConfigFile(), temp));
            archiveUtils.extractDiagnosticArchive(filePath);
            archiveUtils.createArchive(temp, scrubbedName);
         }
         else{
            String scrubbedName = infilePath.substring(pos+1);
            ScrubProcessor scrubber = new ScrubProcessor(inputs.getConfigFile(), temp);
            File targetFile = new File(infilePath);

            BufferedReader br = null;
            BufferedWriter writer = new BufferedWriter(new FileWriter(
                    outputDir + SystemProperties.fileSeparator + scrubbedName));
            br = new BufferedReader(new InputStreamReader(new FileInputStream(targetFile)));

            String thisLine = null;
            while ((thisLine = br.readLine()) != null) {
               thisLine = scrubber.processLine(thisLine);
               writer.write(thisLine);
               writer.newLine();
            }
            writer.close();

            pos = logPath.lastIndexOf(SystemProperties.fileSeparator);
            FileUtils.copyFile(new File(logPath), new File(outputDir + SystemProperties.fileSeparator + logPath.substring(pos+1)));

         }
         closeLogs();
         SystemUtils.nukeDirectory(temp);

      } catch (Throwable t) {
         logger.log(SystemProperties.DIAG, "Error occurred: ", t);
         logger.error("Issue encountered during scrub processing. {}.", Constants.CHECK_LOG);
      }

   }

}