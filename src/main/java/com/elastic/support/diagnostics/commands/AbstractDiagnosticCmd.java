package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.Context;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public abstract class AbstractDiagnosticCmd implements Command {

   protected final Logger logger = LogManager.getLogger();

   public boolean execute(Context context) {
      DiagnosticContext ctx = (DiagnosticContext) context;
      return execute(ctx);
   }

   public abstract boolean execute(DiagnosticContext context);
}
