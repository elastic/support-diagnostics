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

            switch (type){
                case Constants.api :
                    new CheckElasticsearchVersion().execute(context);
                    new CheckUserAuthLevel().execute(context);
                    new CheckPlatformDetails().execute(context);
                    new RunClusterQueries().execute(context);
                    break;

                case Constants.local :
                    new CheckElasticsearchVersion().execute(context);
                    new CheckUserAuthLevel().execute(context);
                    new CheckPlatformDetails().execute(context);
                    new RunClusterQueries().execute(context);
                    if(context.runSystemCalls){
                        new CollectSystemCalls().execute(context);
                        new CollectLogs().execute(context);
                        new RetrieveSystemDigest().execute(context);
                    }
                    if(context.dockerPresent){
                        new CollectDockerInfo().execute(context);
                    }
                    break;

                case Constants.remote :
                    new CheckElasticsearchVersion().execute(context);
                    new CheckUserAuthLevel().execute(context);
                    new CheckPlatformDetails().execute(context);
                    new RunClusterQueries().execute(context);
                    if(context.runSystemCalls){
                        new CollectSystemCalls().execute(context);
                        new CollectLogs().execute(context);
                    }
                    if(context.dockerPresent){
                        new CollectDockerInfo().execute(context);
                    }
                    break;

                case Constants.logstashLocal :
                    new RunLogstashQueries().execute(context);
                    if(context.runSystemCalls){
                        new CollectSystemCalls().execute(context);
                        new RetrieveSystemDigest().execute(context);
                    }
                    if(context.dockerPresent){
                        new CollectDockerInfo().execute(context);
                    }
                    break;

                case Constants.logstashRemote :
                    new RunLogstashQueries().execute(context);
                    if(context.runSystemCalls){
                        new CollectSystemCalls().execute(context);
                    }
                    if(context.dockerPresent){
                        new CollectDockerInfo().execute(context);
                    }
                    break;

                case Constants.logstashApi :
                    new RunLogstashQueries().execute(context);
                    break;
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