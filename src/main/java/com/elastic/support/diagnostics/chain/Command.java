package com.elastic.support.diagnostics.chain;

import com.elastic.support.diagnostics.DiagnosticException;

public interface Command {
    void execute(DiagnosticContext context) throws DiagnosticException;
}
