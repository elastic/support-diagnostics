package com.elastic.support.monitoring;

import com.beust.jcommander.Parameter;
import com.elastic.support.rest.ElasticRestClientInputs;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MonitoringImportInputs extends ElasticRestClientInputs {

    private static final Logger logger = LogManager.getLogger(MonitoringImportInputs.class);

    @Parameter(names = {"--clusterName"}, description = "Overrides the name of the imported cluster.")
    protected String clusterName;
    public String getClusterName() {
        return clusterName;
    }
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    @Parameter(names = {"--indexName"}, description = "Overrides the name of the imported index from the date, appending it to .monitoring-es-7- .")
    protected String indexName = "diag-import" + SystemProperties.getUtcDateString();
    public String getIndexName() {
        return indexName;
    }
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    @Parameter(names = {"-i", "--input"}, required = true, description = "Required: The archive that you wish to import into Elastic Monitoring. This must be in the format produced by the diagnostic export utility.")
    protected String input;
    public String getInput() {
        return input;
    }
    public void setInput(String input) {
        this.input = input;
    }

    public boolean runInteractive(){
        return true;
    }

    public List<String> validate(){

        List<String> errors = new ArrayList<>();
        errors.addAll(super.validate());

        if(StringUtils.isNotEmpty(clusterName)) {
            if (clusterName.contains(" ")) {
                errors.add("Spaces not permitted in cluster name");
            }
        }

        if(StringUtils.isNotEmpty(indexName)) {
            if(indexName.contains(" ")){
                errors.add("Spaces not permitted in index name");
            }
        }

        return errors;
    }
}
