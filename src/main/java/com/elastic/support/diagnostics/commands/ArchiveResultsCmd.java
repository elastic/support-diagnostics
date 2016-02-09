package com.elastic.support.diagnostics.commands;

import com.elastic.support.SystemProperties;
import com.elastic.support.diagnostics.DiagnosticContext;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.util.zip.GZIPOutputStream;


public class ArchiveResultsCmd extends AbstractDiagnosticCmd {

    public boolean execute(DiagnosticContext context){

        try {
            String dir = context.getTempDir();
            File srcDir = new File(dir);

            FileOutputStream fout = new FileOutputStream(dir  + "-" + SystemProperties.getFileDateString() + ".tar.gz");
            GZIPOutputStream gzout = new GZIPOutputStream(fout);
            TarArchiveOutputStream taos = new TarArchiveOutputStream(gzout);

            taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            archiveResults(taos, srcDir, "", true);
            taos.close();

            logger.debug("Archive " + dir + ".zip was created");

        } catch (Exception ioe) {
            logger.error("Couldn't create archive.\n", ioe);
            context.addMessage("Error creating compressed archive from statistics files.");
        }
        return true;
    }

    public void archiveResults(TarArchiveOutputStream taos, File file, String path, boolean append) {

        boolean pathSet = false;
        String relPath = "";

        try {
            if(append) {
                relPath = path + "/" + file.getName() + "-" + SystemProperties.getFileDateString();
            }
            else{
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

    }
}
