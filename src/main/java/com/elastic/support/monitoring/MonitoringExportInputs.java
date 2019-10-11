package com.elastic.support.monitoring;

import com.beust.jcommander.Parameter;
import com.elastic.support.config.ElasticClientInputs;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class MonitoringExportInputs extends ElasticClientInputs {

    private static final Logger logger = LogManager.getLogger(ElasticClientInputs.class);
    private static int defaultInterval = 6;

    @Parameter(names = {"--id"}, description = "Required except when the list command is used: The cluster_uuid of the monitored cluster you wish to extract data for. If you do not know this you can obtain it from that cluster using <protocol>://<host>:port/ .")
    protected String clusterId;
    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    @Parameter(names = {"--interval"}, description = "Number of hours back to collect statistics. Defaults to 6 hours, but but can be set as high as 12.")
    int interval = defaultInterval;
    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getInterval() {
        return interval;
    }

    @Parameter(names = {"--start"}, description = "Date and time for the starting point of the extraction. Defaults to today's date and time, minus the 6 hour default interval in UTC. Must be in the 24 hour format yyyy-MM-dd HH:mm.")
    protected String start = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(ZonedDateTime.now(ZoneId.of("+0")).minusHours(defaultInterval));
    public void setStart(String start) {
        this.start = start;
    }
    public String getStart() {
        return start;
    }

    @Parameter(names = {"--list"}, description = "List the clusters available on the monitoring cluster.")
    boolean listClusters = false;
    public boolean isListClusters() {
        return listClusters;
    }

    public void setListClusters(boolean listClusters) {
        this.listClusters = listClusters;
    }

    // Generated during the validate method for use by the query.
    protected String queryStartDate;
    protected String queryEndDate;


    public boolean validate() {

        if (!super.validate()) {
            return false;
        }

        boolean passed = true;
        if (!listClusters) {
            if (StringUtils.isEmpty(clusterId)) {
                logger.warn("A cluster id is required to extract monitoring data.");
                return false;
            }

            if (interval < 1 || interval > 12) {
                logger.warn("Interval must be between 1 and 12");
                passed = false;
            }

            if (passed == false) {
                return passed;
            }
        }

        try {
            ZonedDateTime workingStart = null, workingStop = null;
            start = start.replace(" ", "T");
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
            logger.warn("Invalid Date or Time format. Please enter the date in format YYYY-MM-dd HH:mm");
            passed = false;
        }

        return passed;
    }
}
