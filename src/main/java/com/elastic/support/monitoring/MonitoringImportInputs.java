package com.elastic.support.monitoring;

import com.beust.jcommander.Parameter;
import com.elastic.support.rest.ElasticRestClientInputs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    protected String indexName;
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

    public boolean validate(){

        if (! super.validate()){
            return false;
        }

        if(clusterName.contains(" ")){
            logger.warn("Spaces not permitted in cluster name");
            return false;
        }

        if(indexName.contains(" ")){
            logger.warn("Spaces not permitted in index name");
            return false;
        }

        return true;
    }
}
