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


}