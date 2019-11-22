package com.elastic.support.diagnostics.chain;

import com.elastic.support.config.Constants;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.diagnostics.commands.*;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DiagnosticChainExec {

    private static Logger logger = LogManager.getLogger(DiagnosticChainExec.class);

    public static void runDiagnostic(DiagnosticContext context, String type) {

        try {
            if("standard".equals(type) || "remote".equals(type)) {
                new GenerateManifestCmd().execute(context);
                new DiagVersionCheckCmd().execute(context);
                new ElasticsearchVersionCheckCmd().execute(context);
                new UserRoleCheckCmd().execute(context);
                new RunClusterQueriesCmd().execute(context);
            }

            if("standard".equals(type)){
                new HostIdentifierCmd().execute(context);
                new CollectLogsCmd().execute(context);
                new SystemCallsCmd().execute(context);
                new SystemDigestCmd().execute(context);
            }

            if("logstash".equals(type)){
                new GenerateManifestCmd().execute(context);
                new DiagVersionCheckCmd().execute(context);
                new RunLogstashQueriesCmd().execute(context);
                new SystemCallsCmd().execute(context);
                new SystemDigestCmd().execute(context);
            }


        } catch (DiagnosticException de) {
            throw de;
        }
        catch (Throwable t){
            logger.log(SystemProperties.DIAG, "Unexpected error", t);
            throw new DiagnosticException(String.format("Fatal error in diagnostic - could not continue. %s", Constants.CHECK_LOG));
        }
    }

}