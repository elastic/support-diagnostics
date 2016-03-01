package com.elastic.support.diagnostics;

import com.elastic.support.chain.Chain;
import com.elastic.support.util.JsonYamlUtils;

import java.util.List;
import java.util.Map;

public class DiagnosticChainExec {

    public void runAnalysis(InspectionContext context) throws Exception{

        Map<String, Object> chains = JsonYamlUtils.readYamlFromClasspath("chains.yml", false);

        List<String> chain = (List)chains.get("analyze");
        Chain analyze = new Chain(chain);
        boolean ret =  analyze.execute(context);
        if (!ret){
            throw new Exception("Error initializing analysis chain");
        }

    }

}