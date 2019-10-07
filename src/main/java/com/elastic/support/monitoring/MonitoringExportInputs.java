package com.elastic.support.monitoring;

import com.beust.jcommander.Parameter;
import com.elastic.support.config.Constants;
import com.elastic.support.config.ElasticClientInputs;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZoneId;
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

    @Parameter(names = {"--startDate"}, description = "Date for the ending day of the extraction. Defaults to today's date. Must be in the format yyyy-MM-dd.")
    protected String startDate = "";
    public String getStartDate() {
        return startDate;
    }
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    @Parameter(names = {"--startTime"}, description = "The clock time you wish to cut off collection statistics. Defaults to 6 the current time. Must be in the format HH:mm.")
    public String startTime = "";
    public String getStartTime() {
        return startTime;
    }
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    @Parameter(names = {"--interval"}, description = "Number of hours back to collect statistics. Defaults to 6 hours, but may be specified up to 12.")
    int interval = 6;

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
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

    public boolean validate(){

        if (! super.validate()){
            return false;
        }

        boolean passed = true;
        if(! listClusters){
            if(StringUtils.isEmpty(clusterId) ){
                logger.warn("A cluster id is required to extract monitoring data.");
                return false;
            }

            if(interval < 1 || interval > 12){
                logger.warn("Interval must be between 1 and 12");
                passed = false;
            }

            if(passed == false){
                return passed;
            }
        }

        try {
            ZonedDateTime start = null, stop = null;
            // Set up the start and stop dates.
            // Adjust for the offset by adding the reversed value given
            // that when you reindex it everything will go in as UTC.
            if(StringUtils.isEmpty(startDate)){
                // Default the stop point to the current datetime, UTC. If the specified an offset so they don't have to manually
                // calculate the UTC diff apply that.
                queryEndDate =  DateTimeFormatter.ofPattern("yyyy-MM-ddTHH:mm").format(ZonedDateTime.now() ) + ":00+00:00";
                stop = ZonedDateTime.parse(queryEndDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                // Since we're using now as a default start date, get the start by moving it back in time by the interval, which may also be a default.
                start = stop.minusHours(interval);
            }
            else{
                queryStartDate= startDate +"T" + startTime + ":00+00:00";
                start = ZonedDateTime.parse(queryStartDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                stop = start.plusHours(interval);
            }

            ZonedDateTime current = ZonedDateTime.now( ZoneId.of("+0") );
            if ( stop.isAfter(current)) {
                logger.info("Warning: The collection interval designates a stopping point after the current date and time. This may result in less data than expected.");
            }

            // Generate the string subs to be used in the query.
            queryStartDate = start.getYear() + "-" + start.getMonthValue() + "-" + start.getDayOfMonth() + "T" + start.getHour() + ":" + start.getMinute() + ":00.000Z";
            queryEndDate =  stop.getYear() + "-" + stop.getMonthValue() + "-" + stop.getDayOfMonth() + "T" + stop.getHour() + ":" + stop.getMinute() + ":00.000Z";

        }
        catch (Exception e){
            logger.warn("Invalid Date or Time format. Please enter the date in format YYYY-MM-dd and the date in HH:mm");
            passed = false;
        }

        return passed;
    }
}
