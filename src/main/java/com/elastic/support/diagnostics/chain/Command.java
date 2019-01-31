package com.elastic.support.diagnostics.chain;

public interface Command {
    void execute(DiagnosticContext context);
}
