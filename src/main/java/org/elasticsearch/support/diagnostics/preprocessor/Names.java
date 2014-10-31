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

/**
 *
 */
public interface Names {
    /**
     * The associated setting should be a valid URL in the form of <code>"http://localhost:9200/"</code> so that all
     * REST API calls can be appended without worry.
     * <p />
     * Default value: <code>"http://localhost:9200/"</code>
     */
    static final String BASE_ELASTICSEARCH_URL = HostPortPreprocessor.BASE_URL_STRING;

    /**
     * The base output directory (represented as a valid {@code Path} directory).
     * <p />
     * Default value: <code>"./support-diagnostics.{hostname}.{nodeName}.{timestamp}"</code> where the {}-enclosed
     * parts are replaced with their respective value.
     */
    static final String OUTPUT_DIRECTORY = OutputDirectoryPreprocessor.PATH_STRING;

    /**
     * The current version of Elasticsearch that is running.
     * <p />
     * Default value: none.
     */
    static final String VERSION = HostPortPreprocessor.VERSION_STRING;
}
