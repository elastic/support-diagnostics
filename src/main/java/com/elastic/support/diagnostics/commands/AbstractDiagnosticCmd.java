package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.DiagnosticCommand;
import com.elastic.support.diagnostics.DiagnosticContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDiagnosticCmd implements DiagnosticCommand {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractDiagnosticCmd.class);

    public abstract boolean execute(DiagnosticContext context);
}
