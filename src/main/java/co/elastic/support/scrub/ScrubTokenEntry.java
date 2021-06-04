/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.scrub;

import java.util.List;
import java.util.regex.Pattern;

public class ScrubTokenEntry {

    public ScrubTokenEntry(String token, List<String> include, List<String> exclude){
        this.token = token;
        this.include = include;
        this.exclude = exclude;
        this.pattern =  Pattern.compile(token);
    }
    public final String token;
    public final List<String> include;
    public final List<String> exclude;
    public final Pattern pattern;

    @Override
    public String toString() {
        return "\nScrubTokenEntry{" +
                "token='" + token + '\'' +
                ", include=" + include +
                ", exclude=" + exclude +
                ", pattern=" + pattern +
                '}';
    }
}
