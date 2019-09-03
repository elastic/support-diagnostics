package com.elastic.support.monitoring;

import com.beust.jcommander.Parameter;
import com.elastic.support.config.Constants;
import com.elastic.support.config.ElasticClientInputs;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class MonitoringExportInputs extends ElasticClientInputs {

    private static final Logger logger = LogManager.getLogger(ElasticClientInputs.class);

    @Parameter(names = {"--id"}, description = "Required except when the list command is used: The cluster_uuid of the monitored cluster you wish to extract data for. If you do not know this you can obtain it from that cluster using <protocol>://<host>:port/ .")
    protected String clusterId;
    public String getClusterId() {
        return clusterId;
    }
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    @Parameter(names = {"--stopDate"}, description = "Date for the ending day of the extraction. Defaults to today's date. Must be in the format yyyy-MM-dd.")
    protected String stopDate = SystemUtils.getCurrentDate();
    public String getStopDate() {
        return stopDate;
    }
    public void setStopDate(String stopDate) {
        this.stopDate = stopDate;
    }

    @Parameter(names = {"--stopTime"}, description = "The clock time you wish to cut off collection statistics. Defaults to 6 the current time. Must be in the format HH:mm.")
    public String stopTime = SystemUtils.getCurrentTime();
    public String getStopTime() {
        return stopTime;
    }
    public void setStopTime(String stopTime) {
        this.stopTime = stopTime;
    }

    @Parameter(names = {"--offset"}, description = "The UTC offset for the time zone you wish to use. Defaults to GMT. Must be in the format +HH:mm or -HH:mm")
    String offset = "+00:00";
    public String getOffset() {
        return offset;
    }
    public void setOffset(String offset) {
        this.offset = offset;
    }

    @Parameter(names = {"--list"}, description = "List the clusters available on the monitoring cluster.")
    boolean listClusters = false;
    public boolean isListClusters() {
        return listClusters;
    }
    public void setListClusters(boolean listClusters) {
        this.listClusters = listClusters;
    }

    protected String queryStartDate;
    protected String queryEndDate;

    @Parameter(names = {"--interval"}, description = "Number of hours back to collect statistics. Defaults to 6 hours, but may be specified up to 12.")
    int interval = 6;

    public boolean validate(){

        if (! super.validate()){
            return false;
        }

        boolean passed = true;

        if(! listClusters && StringUtils.isEmpty(clusterId) ){
            logger.warn("A cluster id is required to extract monitoring data.");
            return false;
        }

        if(! offset.matches(Constants.timeZoneRegex)){
            logger.warn("{} is not a valid time zone offset.");
            passed = false;
        }

        try {
            queryEndDate= stopDate +"T" + stopTime + ":00.000Z";
            ZonedDateTime stopDateTime = ZonedDateTime.parse(stopDate + "T" + stopTime + ":00" + offset, DateTimeFormatter.ISO_DATE_TIME);
            ZonedDateTime start = stopDateTime.minusHours(interval);
            queryStartDate =  start.getYear() + "-" + start.getMonthValue() + "-" + start.getDayOfMonth() + "T" + start.getHour() + ":" + start.getMinute() + ":00.000Z";
        }
        catch (Exception e){
            logger.warn("Invalid Date or Time format. Please enter the date in format YYYY-MM-dd and the date in HH:mm");
            passed = false;
        }

        if(interval < 1 || interval > 12){
            logger.warn("Interval must be between 1 and 12");
            passed = false;
        }

        return passed;
    }

}
