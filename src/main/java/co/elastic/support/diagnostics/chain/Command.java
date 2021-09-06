/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.chain;

import co.elastic.support.diagnostics.DiagnosticException;

public interface Command {
    void execute(DiagnosticContext context) throws DiagnosticException;
}
