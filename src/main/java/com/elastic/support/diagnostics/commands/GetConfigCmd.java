package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.DiagnosticContext;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class GetConfigCmd extends AbstractDiagnosticCmd {

    public boolean execute(DiagnosticContext context) {

        // Get the yaml config file, either default or passed in
        Map configMap = null;
        try {
            configMap = retrieveConfiguration();
        } catch (Exception e) {
            String errorMsg = "Error reading configuration";
            logger.error(errorMsg, e);
            context.addMessage(errorMsg);
            return false;
        }

        context.setConfig(configMap);
        return true;
    }

    public Map retrieveConfiguration() throws Exception {

        InputStream is;
        is = GetConfigCmd.class.getClassLoader().getResourceAsStream("diags.yml");
        return readYaml(is, true);

    }

    public Map readYaml(InputStream inputStream, boolean isBlock) throws Exception {
        Map doc = new LinkedHashMap();

        DumperOptions options = new DumperOptions();
        if (isBlock) {
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        }

        Yaml yaml = new Yaml(options);
        doc = (Map) yaml.load(inputStream);

        return doc;
    }
}
