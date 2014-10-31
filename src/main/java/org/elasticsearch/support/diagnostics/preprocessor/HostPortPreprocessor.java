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
package org.elasticsearch.support.diagnostics.preprocessor;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.elasticsearch.support.diagnostics.DiagnosticToolArgs;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The {@code HostPortPreprocessor} takes the provided host:port from the command line arguments and verifies that they
 * work by extracting the Elasticsearch from the base response.
 */
public class HostPortPreprocessor extends CommandLinePreprocessor {
    /**
     * The key associated with the output directory's path.
     */
    public static final String BASE_URL_STRING = HostPortPreprocessor.class.getName() + ".url";
    /**
     * The key associated with a boolean flag indicating whether the directory was created by this process.
     */
    public static final String VERSION_STRING = HostPortPreprocessor.class.getName() + ".version";

    /**
     * Extracts the version number from the default GET response from "http://host:port/" to Elasticsearch.
     */
    // Note: .*? uses the non-greedy modifier to avoid having to repeat quotes
    public static final Pattern VERSION_EXTRACTOR = Pattern.compile("number['\"]\\s*:\\s*['\"](.*?)['\"]");

    /**
     * The current operating system details.
     */
    private final HttpRequestFactory factory;

    /**
     * Create a new {@link HostPortPreprocessor}.
     *
     * @param args Parsed command line arguments.
     * @param factory The HTTP Request factory used for confirming the URL
     * @throws NullPointerException if any parameter are {@code null}
     */
    @Inject
    HostPortPreprocessor(DiagnosticToolArgs args, HttpRequestFactory factory) {
        super(args);

        // required
        this.factory = checkNotNull(factory, "factory cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImmutableMap<String, String> run(ImmutableMap<String, String> settings) {
        String baseUrl = "http://" + args.getHostPort() + "/";
        // version is parsed from the response of the baseUrl
        String version;

        try {
            HttpResponse response = factory.buildGetRequest(new GenericUrl(baseUrl)).execute();

            // fail fast
            if (response.getStatusCode() != HttpStatusCodes.STATUS_CODE_OK) {
                throw new IOException(
                        "Unable to read from supplied host:port. Status code: " + response.getStatusCode());
            }

            // get the HTTP response to parse the version number; it's a JSON document
            String responseJson = response.parseAsString();
            Matcher matcher = VERSION_EXTRACTOR.matcher(responseJson);

            // if we can't find it, then the response is wrong
            if ( ! matcher.find()) {
                throw new IllegalStateException("Unable to determine Elasticsearch version. Checked " + baseUrl);
            }

            // extract the version from the response
            version = matcher.group(1);
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to determine valid Elasticsearch URL. Tried " + baseUrl, e);
        }

        return ImmutableMap.of(BASE_URL_STRING, baseUrl, VERSION_STRING, version);
    }
}
