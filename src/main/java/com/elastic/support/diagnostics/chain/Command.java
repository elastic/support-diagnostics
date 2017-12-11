package com.elastic.support.diagnostics.chain;

public interface Command {
    public boolean execute(DiagnosticContext context);
}
