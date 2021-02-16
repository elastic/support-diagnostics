package com.elastic.support.monitoring;

import com.beust.jcommander.Parameter;
import com.elastic.support.rest.ElasticRestClientInputs;
import com.elastic.support.util.ResourceUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.beryx.textio.StringInputReader;
import java.util.Collections;
import java.util.List;

public class MonitoringImportInputs extends ElasticRestClientInputs {

    private static final Logger logger = LogManager.getLogger(MonitoringImportInputs.class);

    // Start Input Fields

    @Parameter(names = {"-clustername"}, description = "Overrides the name of the imported cluster.")
    protected String clusterName;

    @Parameter(names = {"-input"}, description = "Required: The archive that you wish to import into Elastic Monitoring. This must be in the format produced by the diagnostic export utility.")
    protected String input;

    // End Input Fields

    public MonitoringImportInputs(String delimiter){
        super(delimiter);
    }

    public void runInteractive(){

        clusterName = ResourceUtils.textIO.newStringInputReader()
                .withMinLength(0)
                .read("Specify an alternate name for the imported cluster or hit enter to use original cluster name:");

        input = ResourceUtils.textIO.newStringInputReader()
                .withInputTrimming(true)
                .withValueChecker((String val, String propname) -> validateRequiredFile(val))
                .read("Enter the full path of the archvive you wish to import.");

        runHttpInteractive();
        ResourceUtils.textIO.dispose();

    }

    public List<String> parseInputs(String[] args){

        List<String> errors = super.parseInputs(args);

        errors.addAll(ObjectUtils.defaultIfNull(validateId(clusterName), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validateRequiredFile(input), emptyList));

        return errors;

    }

    public List<String> validateId(String val){
        if(StringUtils.isEmpty(val)){
            return null;
        }
        if(val.contains(" ")){
            return Collections.singletonList("Spaces not permitted in name.");
        }
        return null;
    }

    @Override
    public String toString() {
        return "MonitoringImportInputs{" +
                "clusterName='" + clusterName + '\'' +
                ", input='" + input + '\'' +
                '}';
    }
}
