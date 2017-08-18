package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

public class ThreadDumpCmd extends BaseSystemCallsCmd {

   public boolean execute( DiagnosticContext context) {

      Map<String, String> cmds = (Map<String, String>) context.getConfig().get("thread-dump");
      executeCalls(cmds, context);

      return true;

   }
}

