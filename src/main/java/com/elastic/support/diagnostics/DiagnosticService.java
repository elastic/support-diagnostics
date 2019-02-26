package com.elastic.support.diagnostics;

import com.elastic.support.BaseService;
import com.elastic.support.diagnostics.chain.DiagnosticChainExec;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.diagnostics.chain.GlobalContext;
import com.elastic.support.config.DiagnosticInputs;
import com.elastic.support.rest.RestExec;
import com.elastic.support.util.ArchiveUtils;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DiagnosticService extends BaseService {

    public DiagnosticService(DiagnosticInputs diagnosticInputs){

        super();

        try {

            GlobalContext.restExec = new RestExec(diagnosticInputs, restSettings);


        } catch (Exception e) {
            logger.error("Error encountered running diagnostic. See logs for additional information.  Exiting application.", e);
            throw new RuntimeException("DiagnosticService runtime error", e);
        }
    }

    public void run() {

        if (GlobalContext.getDiagnosticInputs().getReps() == 1) {
            exec();
        } else {
            try {
                for (int r = 1; r <= GlobalContext.getDiagnosticInputs().getReps(); r++) {
                    exec();
                    Thread.sleep(GlobalContext.getDiagnosticInputs().getInterval() * 60 * 1000);
                }
            } catch (Exception e) {
                logger.error("Worker error executing multiple diag runs");
            }
        }
    }

    public void exec() {

        DiagnosticChainExec dc = new DiagnosticChainExec();
        DiagnosticContext ctx = new DiagnosticContext();
        DiagnosticInputs diagnosticInputs = GlobalContext.getDiagnosticInputs();

        // Set up the output directory
        String tempDir = diagnosticInputs.getTempDir();
        logger.info("Creating temp directory: {}", tempDir);


        // Create the temp directory - delete if first if it exists from a previous run
        try {
            logger.info("Creating temporary directory: {}.", tempDir);

            FileUtils.deleteDirectory(new File(tempDir));
            Files.createDirectories(Paths.get(tempDir));
            ctx.setTempDir(tempDir);

            logger.info("Created temp directory: {}", ctx.getTempDir());

        } catch (IOException e) {
            logger.error("Access issue with temp directory", e);
            throw new RuntimeException("Issue with creating temp directory - see logs for details.");
        }

        logger.info("Configuring log file.");

        createFileAppender(tempDir, "diagnostics.log");

        dc.runDiagnostic(ctx);
        closeLogs();
        createArchive(ctx);
        SystemUtils.nukeDirectory(tempDir);

    }


    private void createArchive(DiagnosticContext context) {

        logger.info("Archiving diagnostic results.");

        try {
            String archiveFilename = SystemProperties.getFileDateString();
            String dir = context.getTempDir();
            ArchiveUtils archiveUtils = new ArchiveUtils();
            archiveUtils.createArchive(dir, archiveFilename);

        } catch (Exception ioe) {
            logger.error("Couldn't create archive. {}", ioe);
        }

    }

    public void close(){
        GlobalContext.getRestExec().close();
    }

}
