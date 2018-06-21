package com.elastic.support.diagnostics;

import com.beust.jcommander.Parameter;
import com.elastic.support.util.SystemProperties;


public class ScrubInputParams {

   @Parameter(names = {"-a", "--archive",}, required = true, description = "Required field.  Full path to the archive file to be scrubbed.")
   private String archive;

   public String getArchive() {
      return archive;
   }

   public void setArchive(String archive) {
      this.archive = archive;
   }

   @Parameter(names = {"-f", "--configFile",}, required = false, description = "Optional field.  Full path to the file where string tokens you wish to have removed resides.")
   private String scrubFile = SystemProperties.userDir + SystemProperties.fileSeparator +  "scrub.yml";

   public String getScrubFile() {
      return scrubFile;
   }

   public void setScrubFile(String scrubFile) {
      this.scrubFile = scrubFile;
   }

   @Parameter(names = {"-t", "--target",}, required = false, description = "Optional field.  Full path to the directory where the scrubbed archive will be written.")
   private String targetDir;

   public String getTargetDir() {
      return targetDir;
   }

   public void setTargetDir(String targetDir) {
      this.targetDir = targetDir;
   }

   @Parameter(names = {"-?", "--help"}, description = "Help contents.", help = true)
   private boolean help;

   public boolean isHelp() {
      return help;
   }

   public void setHelp(boolean help) {
      this.help = help;
   }
}