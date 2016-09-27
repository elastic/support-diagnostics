package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.Context;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDiagnosticCmd implements Command {

   protected static final Logger logger = LoggerFactory.getLogger(AbstractDiagnosticCmd.class);

   public boolean execute(Context context) {
      DiagnosticContext ctx = (DiagnosticContext) context;
      return execute(ctx);
   }

   public abstract boolean execute(DiagnosticContext context);
}
