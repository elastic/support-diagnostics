package com.elastic.support.diagnostics;

import com.elastic.support.diagnostics.chain.DiagnosticChainExec;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.diagnostics.chain.GlobalContext;
import com.elastic.support.util.ArchiveUtils;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Diagnostic extends BaseDiagnostic {


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
        logger.info("Creating temp directory.");

        // If an output directory was not specified use the current working directory.
        String outputDir = SystemProperties.userDir;
        if (diagnosticInputs.getOutputDir() != null) {
            outputDir = diagnosticInputs.getOutputDir();
        }

        logger.info("Results will be written to: " + outputDir);

        String diagName = Constants.ES_DIAG;
        String diagType = diagnosticInputs.getDiagType();
        if (!diagType.equals(Constants.ES_DIAG_DEFAULT)) {
            diagName = diagType + "-" + Constants.ES_DIAG;
        }
        String tempDir = outputDir + SystemProperties.fileSeparator + diagName;

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
        nukeTempDir(tempDir);

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
