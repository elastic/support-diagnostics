/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.chain;

import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.diagnostics.commands.CheckDiagnosticVersion;
import co.elastic.support.diagnostics.commands.CheckElasticsearchVersion;
import co.elastic.support.diagnostics.commands.CheckKibanaVersion;
import co.elastic.support.diagnostics.commands.CheckPlatformDetails;
import co.elastic.support.diagnostics.commands.CheckUserAuthLevel;
import co.elastic.support.diagnostics.commands.CollectDockerInfo;
import co.elastic.support.diagnostics.commands.CollectKibanaLogs;
import co.elastic.support.diagnostics.commands.CollectLogs;
import co.elastic.support.diagnostics.commands.CollectSystemCalls;
import co.elastic.support.diagnostics.commands.GenerateDiagnosticManifest;
import co.elastic.support.diagnostics.commands.GenerateLogstashDiagnostics;
import co.elastic.support.diagnostics.commands.GenerateManifest;
import co.elastic.support.diagnostics.commands.KibanaGetDetails;
import co.elastic.support.diagnostics.commands.RetrieveSystemDigest;
import co.elastic.support.diagnostics.commands.RunClusterQueries;
import co.elastic.support.diagnostics.commands.RunKibanaQueries;
import co.elastic.support.diagnostics.commands.RunLogstashQueries;
import co.elastic.support.Constants;

public class DiagnosticChainExec {

    public static void runDiagnostic(DiagnosticContext context, String type) throws DiagnosticException {
        new CheckDiagnosticVersion().execute(context);

        switch (type) {
            case Constants.api:
                new CheckElasticsearchVersion().execute(context);
                new CheckUserAuthLevel().execute(context);
                // Removed temporarily due to issues with finding and accessing cloud master
                // new CheckPlatformDetails().execute(context);
                new RunClusterQueries().execute(context);
                break;

            case Constants.local:
                new CheckElasticsearchVersion().execute(context);
                new CheckUserAuthLevel().execute(context);
                new CheckPlatformDetails().execute(context);
                new RunClusterQueries().execute(context);
                if (context.runSystemCalls) {
                    new CollectSystemCalls().execute(context);
                    new CollectLogs().execute(context);
                    new RetrieveSystemDigest().execute(context);
                }
                if (context.dockerPresent) {
                    new CollectDockerInfo().execute(context);
                }
                break;

            case Constants.remote:
                new CheckElasticsearchVersion().execute(context);
                new CheckUserAuthLevel().execute(context);
                new CheckPlatformDetails().execute(context);
                new RunClusterQueries().execute(context);
                if (context.runSystemCalls) {
                    new CollectSystemCalls().execute(context);
                    new CollectLogs().execute(context);
                }
                if (context.dockerPresent) {
                    new CollectDockerInfo().execute(context);
                }
                break;

            case Constants.logstashLocal:
                new RunLogstashQueries().execute(context);
                new GenerateLogstashDiagnostics().execute(context);
                if (context.runSystemCalls) {
                    new CollectSystemCalls().execute(context);
                    new RetrieveSystemDigest().execute(context);
                }
                if (context.dockerPresent) {
                    new CollectDockerInfo().execute(context);
                }
                break;

            case Constants.logstashRemote:
                new RunLogstashQueries().execute(context);
                new GenerateLogstashDiagnostics().execute(context);
                if (context.runSystemCalls) {
                    new CollectSystemCalls().execute(context);
                }
                if (context.dockerPresent) {
                    new CollectDockerInfo().execute(context);
                }
                break;

            case Constants.logstashApi:
                new RunLogstashQueries().execute(context);
                break;

            case Constants.kibanaLocal:
                new CheckKibanaVersion().execute(context);
                new KibanaGetDetails().execute(context);
                new RunKibanaQueries().execute(context);
                if (context.runSystemCalls) {
                    new CollectSystemCalls().execute(context);
                    new CollectKibanaLogs().execute(context);
                    new RetrieveSystemDigest().execute(context);
                }
                if (context.dockerPresent) {
                    new CollectDockerInfo().execute(context);
                }
                break;

            case Constants.kibanaRemote:
                new CheckKibanaVersion().execute(context);
                new KibanaGetDetails().execute(context);
                new RunKibanaQueries().execute(context);
                if (context.runSystemCalls) {
                    new CollectSystemCalls().execute(context);
                    new CollectKibanaLogs().execute(context);
                }
                if (context.dockerPresent) {
                    new CollectDockerInfo().execute(context);
                }
                break;

            case Constants.kibanaApi:
                new CheckKibanaVersion().execute(context);
                new RunKibanaQueries().execute(context);
                break;
        }

        new GenerateManifest().execute(context);
        new GenerateDiagnosticManifest().execute(context);
    }

}