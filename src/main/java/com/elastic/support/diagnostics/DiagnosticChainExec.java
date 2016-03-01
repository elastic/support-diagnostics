package com.elastic.support.diagnostics;

import com.elastic.support.chain.Chain;
import com.elastic.support.util.JsonYamlUtils;

import java.util.List;
import java.util.Map;

public class DiagnosticChainExec {

   public void runDiagnostic(DiagnosticContext context) throws Exception {

      Map<String, Object> diags = JsonYamlUtils.readYamlFromClasspath("diags.yml", true);
      context.setConfig(diags);

      Map<String, Object> chains = JsonYamlUtils.readYamlFromClasspath("chains.yml", false);
      List<String> chain = (List) chains.get(context.getInputParams().getDiagType());
      Chain analyze = new Chain(chain);
      boolean ret = analyze.execute(context);
      if (!ret) {
         throw new Exception("Error initializing diagnostic chain");
      }

   }

}