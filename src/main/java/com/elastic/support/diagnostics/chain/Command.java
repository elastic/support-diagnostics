/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package com.elastic.support.diagnostics.chain;

import com.elastic.support.diagnostics.DiagnosticException;

public interface Command {
    void execute(DiagnosticContext context) throws DiagnosticException;
}
