/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package com.elastic.support.diagnostics.chain;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagnosticException;
import com.elastic.support.diagnostics.commands.*;

public class DiagnosticChainExec {

    public static void runDiagnostic(DiagnosticContext context, String type) throws DiagnosticException {
        new CheckDiagnosticVersion().execute(context);

        switch (type){
            case Constants.api :
                new CheckElasticsearchVersion().execute(context);
                new CheckUserAuthLevel().execute(context);
                // Removed temporarily due to issues with finding and accessing cloud master
                //new CheckPlatformDetails().execute(context);
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

            case Constants.kibanaLocal :
                new CheckKibanaVersion().execute(context);
                new KibanaGetDetails().execute(context);
                new RunKibanaQueries().execute(context);
                if(context.runSystemCalls){
                    new CollectSystemCalls().execute(context);
                    new CollectKibanaLogs().execute(context);
                    new RetrieveSystemDigest().execute(context);
                }
                if(context.dockerPresent){
                    new CollectDockerInfo().execute(context);
                }
                break;

            case Constants.kibanaRemote :
                new CheckKibanaVersion().execute(context);
                new KibanaGetDetails().execute(context);
                new RunKibanaQueries().execute(context);
                if(context.runSystemCalls){
                    new CollectSystemCalls().execute(context);
                    new CollectKibanaLogs().execute(context);
                }
                if(context.dockerPresent){
                    new CollectDockerInfo().execute(context);
                }
                break;

             case Constants.kibanaApi :
                new CheckKibanaVersion().execute(context);
                new RunKibanaQueries().execute(context);
                break;
            }

        new GenerateManifest().execute(context);
    }

}