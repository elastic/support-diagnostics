package com.elastic.support.diagnostics.commands;

import com.elastic.support.util.SystemProperties;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;


public class ArchiveResultsCmd extends AbstractDiagnosticCmd {

   public boolean execute(DiagnosticContext context) {

/*      logger.info("Archiving diagnostic results.");

      try {
         String archiveFilename = SystemProperties.getFileDateString();
         if(context.getInputParams().getReps() > 1){
            int currentRep = context.getCurrentRep();
            if(currentRep == 1){
               context.setAttribute("archiveFileName", archiveFilename);
            }

            archiveFilename = context.getStringAttribute("archiveFileName") + "-run-" + currentRep;
         }

         boolean bzip = context.getInputParams().isBzip();
         String ext = "";
         if (bzip){
            ext = ".bz2";
         }
         else{
            ext = ".gz";
         }

         String dir = context.getTempDir();
         File srcDir = new File(dir);
         String filename = dir + "-" + archiveFilename + ".tar" + ext;

         FileOutputStream fout = new FileOutputStream(filename);
         CompressorOutputStream cout = null;
         if(bzip){
            cout = new BZip2CompressorOutputStream(fout);
         }
         else {
            cout = new GzipCompressorOutputStream(fout);
         }
         TarArchiveOutputStream taos = new TarArchiveOutputStream(cout);

         taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
         taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
         archiveResults(taos, srcDir, "", true);
         taos.close();

         logger.info("Archive: " + filename + " was created");

      } catch (Exception ioe) {
         logger.error("Couldn't create archive.\n", ioe);
      }*/
      return true;
   }

/*   public void archiveResults(TarArchiveOutputStream taos, File file, String path, boolean append) {

      boolean pathSet = false;
      String relPath = "";

      try {
         if (append) {
            relPath = path + "/" + file.getName() + "-" + SystemProperties.getFileDateString();
         } else {
            relPath = path + "/" + file.getName();
         }
         TarArchiveEntry tae = new TarArchiveEntry(file, relPath);
         taos.putArchiveEntry(tae);

         if (file.isFile()) {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            IOUtils.copy(bis, taos);
            taos.closeArchiveEntry();
            bis.close();

         } else if (file.isDirectory()) {
            taos.closeArchiveEntry();
            for (File childFile : file.listFiles()) {
               archiveResults(taos, childFile, relPath, false);
            }
         }

      } catch (IOException e) {
         logger.error("Archive Error", e);
      }

   }*/
}
