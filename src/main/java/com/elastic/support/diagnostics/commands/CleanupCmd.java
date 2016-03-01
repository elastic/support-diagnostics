package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.DiagnosticContext;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class CleanupCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

      logger.info("Deleting temporary directories.");
      String dir = context.getTempDir();
      try {
         FileUtils.deleteDirectory(new File(dir));
      } catch (IOException e) {
         String msg = "Error deleting temporary work directory";
         logger.error(msg, e);
      }
      logger.debug("Temp directory " + dir + " was deleted.");

      return true;
   }
}
