/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.support.diagnostics;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.CliFactory;

import org.elasticsearch.support.diagnostics.cat.CatModule;
import org.elasticsearch.support.diagnostics.cluster.ClusterModule;
import org.elasticsearch.support.diagnostics.indices.IndicesModule;
import org.elasticsearch.support.diagnostics.preprocessor.PreprocessorModule;

/**
 * {@code DiagnosticTool} represents the main entry point into the Elasticsearch Diagnostics tool and it will kick off
 * a series of {@code DiagnosticProcess}es that output files until culminating in a post-processor that will tar/zip
 * the contents.
 */
public class DiagnosticTool {

    /**
     * Run the diagnostics tool.
     *
     * @param args Incoming arguments passed to the application.
     */
    public static void main(String[] args) {
        // parse command line arguments
        DiagnosticToolArgs parsedArgs = null;

        try {
            parsedArgs = CliFactory.parseArguments(DiagnosticToolArgs.class, args);
        }
        catch (ArgumentValidationException e) {
            // display the error
            System.out.println(e.getMessage());
            // bad request (and a vague idea of how bad)
            System.exit(400 + e.getValidationFailures().size());
        }

        // prepare to run application (setup Guice)
        Injector injector =
                Guice.createInjector(
                        new DiagnosticsModule(parsedArgs),
                        new PreprocessorModule(),
                        new CatModule(),
                        new IndicesModule(),
                        new ClusterModule());

        // run and exit
        System.exit(injector.getInstance(DiagnosticsRunner.class).run());
    }
}
