/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package co.elastic.support.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;

/**
 * {@code UrlUtils} contains helpful methods for dealing with URLs.
 */
public class UrlUtils {
    /**
     * URL Encode the {@code value}.
     *
     * @param value The value to URL encode.
     * @return Never {@code null}.
     * @throws RuntimeException if encoding throws an exception.
     */
    public static String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
