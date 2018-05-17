package com.elastic.support.diagnostics;

import com.beust.jcommander.*;


public class ScrubInputParams {

   @Parameter(names = {"-a", "--archive",}, required = true, description = "Required field.  Full path to the archive file to be scrubbed.")
   private String archive;

   public String getArchive() {
      return archive;
   }

   public void setArchive(String archive) {
      this.archive = archive;
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