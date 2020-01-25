package com.elastic.support.monitoring;

import com.beust.jcommander.Parameter;
import com.elastic.support.rest.ElasticRestClientInputs;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.beryx.textio.StringInputReader;
import org.beryx.textio.TerminalProperties;
import org.beryx.textio.TextTerminal;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class MonitoringExportInputs extends ElasticRestClientInputs {

    private static final Logger logger = LogManager.getLogger(ElasticRestClientInputs.class);
    private static int defaultInterval = 6;

    @Parameter(names = {"--id"}, description = "Required except when the list command is used: The cluster_uuid of the monitored cluster you wish to extract data for. If you do not know this you can obtain it from that cluster using <protocol>://<host>:port/ .")
    public String clusterId;

    @Parameter(names = {"--start"}, description = "Date and time for the starting point of the extraction. Defaults to today's date and time, minus the 6 hour default interval in UTC. Must be in the 24 hour format yyyy-MM-dd HH:mm.")
    public String start = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(ZonedDateTime.now(ZoneId.of("+0")).minusHours(defaultInterval));

    @Parameter(names = {"--interval"}, description = "Number of hours back to collect statistics. Defaults to 6 hours, but but can be set as high as 12.")
    public int interval = defaultInterval;

    @Parameter(names = {"--list"}, description = "List the clusters available on the monitoring cluster.")
    boolean listClusters = false;

    // Generated during the validate method for use by the query.
    public String queryStartDate;
    public String queryEndDate;

    public boolean runInteractive() {

        String operation = standardStringReader
                .withNumberedPossibleValues("List", "Extract.")
                .withIgnoreCase()
                .read("List monitored clusters available or extract data from a cluster.");

        if(operation.equals("extract")){
            clusterId = textIO.newStringInputReader()
                    .withInputTrimming(true)
                    .withValueChecker((String val, String propname) -> validateCluster(val))
                    .read("Enter the cluster id to for the cluster you wish to extract.");

            interval = textIO.newIntInputReader()
                    .withInputTrimming(true)
                    .withDefaultValue(interval)
                    .withValueChecker((Integer val, String propname) -> validateInterval(val))
                    .read("Enter the cluster id to for the cluster you wish to extract.");

            terminal.println("\"Date and time for the earliest point of the extraction.");
            terminal.println("Defaults to today's date and time, minus the 6 hour default interval in UTC.");
            terminal.println("Must be in the 24 hour format yyyy-MM-dd HH:mm.");

            start = textIO.newStringInputReader()
                    .withInputTrimming(true)
                    .withDefaultValue(start)
                    .withValueChecker((String val, String propname) -> validateStart(val))
                    .read("Enter the cluster id to for the cluster you wish to extract.");
        }

        runHttpInteractive();
        runOutputDirInteractive();

        return true;
    }

    public List<String> parseInputs(String[] args) {
        List<String> errors = parseInputs(args);

        if (!listClusters) {
            errors.addAll(ObjectUtils.defaultIfNull(validateCluster(clusterId), emptyList));
            errors.addAll(ObjectUtils.defaultIfNull(validateInterval(interval), emptyList));
            errors.addAll(ObjectUtils.defaultIfNull(validateStart(start), emptyList));
        }

        return errors;

    }

    public List<String> validateStart(String val){

        try {
            ZonedDateTime workingStart = null, workingStop = null;
            start = val.replace(" ", "T");
            workingStart = ZonedDateTime.parse(start + ":00+00:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            workingStop = workingStart.plusHours(interval);

            ZonedDateTime current = ZonedDateTime.now(ZoneId.of("+0"));
            if (workingStop.isAfter(current)) {
                logger.info("Warning: The input collection interval designates a stopping point after the current date and time. This may result in less data than expected.");
                workingStop = current;
            }

            // Generate the string subs to be used in the query.
            queryStartDate = (DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(workingStart) + ":00.000Z").replace(" ", "T");
            queryEndDate = (DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(workingStop) + ":00.000Z").replace(" ", "T");

        } catch (Exception e) {
            return Collections.singletonList("Invalid Date or Time format. Please enter the date in format YYYY-MM-dd HH:mm");
        }

        return emptyList;

    }

    public List<String> validateCluster(String val){
        if(StringUtils.isEmpty(val)){
            return Collections.singletonList("Cluster id is required to extract monitoring data.");
        }
        return emptyList;
    }

    public List<String> validateInterval(int val){
        if(val <1 || val > 12){
            return Collections.singletonList("Interval must be 1-12.");
        }
        return emptyList;
    }



}
