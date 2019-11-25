package com.elastic.support.diagnostics.chain;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.diagnostics.commands.*;
import com.elastic.support.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DiagnosticChainExec {

    private static Logger logger = LogManager.getLogger(DiagnosticChainExec.class);

    public static void runDiagnostic(DiagnosticContext context, String type) {

        try {
            new CheckDiagnosticVersion().execute(context);

            if(Constants.local.equals(type) || Constants.localApi.equals(type)) {
                new CheckElasticsearchVersion().execute(context);
                new CheckUserAuthLevel().execute(context);
                new RunClusterQueries().execute(context);
            }

            if(Constants.local.equals(type)){
                new VerifyHostType().execute(context);
                new CollectLocalLogs().execute(context);
                new CollectLocalSystemCalls().execute(context);
                new RetrieveSystemDigest().execute(context);
            }

            if(Constants.logstash.equals(type)){
                new RunLogstashQueries().execute(context);
                new CollectLocalSystemCalls().execute(context);
                new RetrieveSystemDigest().execute(context);
            }

            new GenerateManifest().execute(context);
            
        } catch (DiagnosticException de) {
            throw de;
        }
        catch (Throwable t){
            logger.log(SystemProperties.DIAG, "Unexpected error", t);
            throw new DiagnosticException(String.format("Fatal error in diagnostic - could not continue. %s", Constants.CHECK_LOG));
        }
    }

}