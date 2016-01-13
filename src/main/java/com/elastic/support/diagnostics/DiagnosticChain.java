package com.elastic.support.diagnostics;

import com.elastic.support.diagnostics.commands.*;

import java.util.ArrayList;
import java.util.List;


public class DiagnosticChain implements DiagnosticCommand {

   public boolean execute(DiagnosticContext context) {

      //Set up initial generic chain
      List<DiagnosticCommand> commandChain = new ArrayList<DiagnosticCommand>();
      commandChain.add(new GetConfigCmd());
      commandChain.add(new HostIdentifierCmd());
      commandChain.add(new InteractiveInputCmd());
      commandChain.add(new RestModuleSetupCmd());
      commandChain.add(new VersionAndClusterNameCheckCmd());
      commandChain.add(new DirectorySetupCmd());
      commandChain.add(new RunClusterQueriesCmd());
      commandChain.add(new ExtractNodeInfoCmd());
      commandChain.add(new GenerateManifestCmd());
      commandChain.add(new LogAndConfigCmd());
      commandChain.add(new SystemCallsCmd());
      commandChain.add(new ArchiveResultsCmd());
      commandChain.add(new CleanupCmd());

      for (DiagnosticCommand dc : commandChain) {
         boolean rc = dc.execute(context);
         if (!rc) {
            return rc;
         }
      }

      return true;
   }


}
