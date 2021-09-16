/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.monitoring;

import co.elastic.support.util.SystemProperties;
import co.elastic.support.util.TextIOManager;
import com.beust.jcommander.Parameter;
import co.elastic.support.Constants;
import co.elastic.support.rest.ElasticRestClientInputs;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class MonitoringExportInputs extends ElasticRestClientInputs {

    private static final Logger logger = LogManager.getLogger(ElasticRestClientInputs.class);
    private static int defaultInterval = 6;

    // Start Input Fields
    @Parameter(names = {"--id"}, description = "Required except when the list command is used: The cluster_uuid of the monitored cluster you wish to extract data for. If you do not know this you can obtain it from that cluster using <protocol>://<host>:port/ .")
    public String clusterId;

    @Parameter(names = {"--cutoffDate"}, description = "Date for the cutoff point of the extraction. Defaults to today. Must be in the 24 hour format yyyy-MM-dd.")
    public String cutoffDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(ZonedDateTime.now(ZoneId.of("+0")));;

    @Parameter(names = {"--cutoffTime"}, description = "Time for the cutoff point of the data extraction. Defaults to today's current UTC time, and the starting point will be calculated as that minus the configured interval. Must be in the 24 hour format HH:mm.")
    public String cutoffTime = DateTimeFormatter.ofPattern("HH:mm").format(ZonedDateTime.now(ZoneId.of("+0")));;;

    @Parameter(names = {"--interval"}, description = "Number of hours back to collect statistics. Defaults to 6 hours, but but can be set as high as 12.")
    public int interval = defaultInterval;

    @Parameter(names = {"--list"}, description = "List the clusters available on the monitoring cluster.")
    boolean listClusters = false;

    @Parameter(names = {"--type"}, description = "Enter monitoring, metrics, or all.")
    String type = "monitoring";

    // End Input Fields


    // Generated during the validate method for use by the query.
    public String queryStartDate;
    public String queryEndDate;

    public boolean runInteractive(TextIOManager textIOManager) {

        runHttpInteractive(textIOManager);

        String operation = textIOManager.standardStringReader
                .withNumberedPossibleValues("List", "Extract")
                .withIgnoreCase()
                .read(SystemProperties.lineSeparator + "List monitored clusters available or extract data from a cluster." );

        operation = operation.toLowerCase();
        if(operation.equals("extract")){
            type = textIOManager.textIO.newStringInputReader()
                    .withInputTrimming(true)
                    .withDefaultValue("monitoring")
                    .read(SystemProperties.lineSeparator + "Enter monitoring for ES and Logstash monitoring data, metric for metricbeat system data, or all.");

            clusterId = textIOManager.textIO.newStringInputReader()
                    .withInputTrimming(true)
                    .withValueChecker((String val, String propname) -> validateCluster(val))
                    .read(SystemProperties.lineSeparator + "Enter the cluster id to for the cluster you wish to extract.");

            cutoffDate = textIOManager.textIO.newStringInputReader()
                    .withInputTrimming(true)
                    .withMinLength(0)
                    .read(SystemProperties.lineSeparator + "Enter the date for the cutoff point of the extraction. Defaults to today's date. Must be in the format yyyy-MM-dd.");

            cutoffTime = textIOManager.textIO.newStringInputReader()
                    .withInputTrimming(true)
                    .withMinLength(0)
                    .read(SystemProperties.lineSeparator + "Enter the time for the cutoff point of the extraction. Defaults to the current UTC time. Must be in the 24 hour format HH:mm.");

            interval = textIOManager.textIO.newIntInputReader()
                    .withInputTrimming(true)
                    .withDefaultValue(interval)
                    .withValueChecker((Integer val, String propname) -> validateInterval(val))
                    .read(SystemProperties.lineSeparator + "The number of hours you wish to extract. Whole integer values only. Defaults to 6 hours.");

            validateTimeWindow();
        }

        runOutputDirInteractive(textIOManager);

        return true;
    }

    @Override
    public List<String> parseInputs(TextIOManager textIOManager, String[] args) {
        List<String> errors = super.parseInputs(textIOManager, args);

        if (!listClusters) {
            errors.addAll(ObjectUtils.defaultIfNull(validateCluster(clusterId), emptyList));
            errors.addAll(ObjectUtils.defaultIfNull(validateInterval(interval), emptyList));
            errors.addAll(ObjectUtils.defaultIfNull(validateTimeWindow(), emptyList));
        }

        return errors;
    }

    public List<String> validateTimeWindow(){

        ZonedDateTime start, end;
        try {
            String cutoff = cutoffDate + "T" + cutoffTime;
            end = ZonedDateTime.parse(cutoff + ":00+00:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            ZonedDateTime current = ZonedDateTime.now(ZoneId.of("+0"));
            if (end.isAfter(current)) {
                logger.warn(Constants.CONSOLE, "Warning: The input collection interval designates a stopping point after the current date and time. Resetting the start to the current date/time.");
                end = current;
            }

            // Generate the string subs to be used in the query.
            queryEndDate = cutoff +  ":00.000Z";

            start = end.minusHours(interval);
            queryStartDate = (DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(start) + ":00.000Z").replace(" ", "T");

        } catch (Exception e) {
            return Collections.singletonList("Invalid Date or Time format. Please enter the date in format YYYY-MM-dd HH:mm");
        }

        return null;
    }

    public List<String> validateCluster(String val){
        if(StringUtils.isEmpty(val)){
            return Collections.singletonList("Cluster id is required to extract monitoring data.");
        }
        return null;
    }

    public List<String> validateInterval(int val){
        if(val <1 || val > 12){
            return Collections.singletonList("Interval must be 1-12.");
        }
        return null;
    }

    @Override
    public String toString() {
        return "MonitoringExportInputs{" +
                "clusterId='" + clusterId + '\'' +
                ", cutoffDate='" + cutoffDate + '\'' +
                ", cutoffTime='" + cutoffTime + '\'' +
                ", interval=" + interval +
                ", listClusters=" + listClusters +
                ", queryStartDate='" + queryStartDate + '\'' +
                ", queryEndDate='" + queryEndDate + '\'' +
                '}';
    }
}
