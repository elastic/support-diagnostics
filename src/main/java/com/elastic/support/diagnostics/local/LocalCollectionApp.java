package com.elastic.support.diagnostics.local;

import com.elastic.support.config.Constants;
import com.elastic.support.diagnostics.DiagConfig;
import com.elastic.support.config.LocalCollectionInputs;
import com.elastic.support.util.JsonYamlUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class LocalCollectionApp {

    private static final Logger logger = LogManager.getLogger(LocalCollectionApp.class);

    public static void main(String[] args) {

        LocalCollectionInputs inputs = new LocalCollectionInputs();
        inputs.parseInputs(args);


        Map diagMap = JsonYamlUtils.readYamlFromClasspath(Constants.DIAG_CONFIG, true);
        DiagConfig diagConfig = new DiagConfig(diagMap);

        LocalCollectionService collection = new LocalCollectionService();
        collection.exec(inputs, diagConfig);

    }

}



