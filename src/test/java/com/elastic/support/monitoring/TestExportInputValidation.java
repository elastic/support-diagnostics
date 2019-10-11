package com.elastic.support.monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertEquals;

public class TestExportInputValidation {

    private static final Logger logger = LogManager.getLogger(TestExportInputValidation.class);

    @Test
    public void testExportInputValidations(){

        // Check cluster id validation
        MonitoringExportInputs mei = new MonitoringExportInputs();
        boolean valid = mei.validate();
        assertEquals(false, valid);

        mei = new MonitoringExportInputs();
        mei.setClusterId("test");
        mei.setStart("08-29-2019 02:25");
        valid = mei.validate();
        assertEquals(false, valid);

        mei = new MonitoringExportInputs();
        mei.setClusterId("test");
        mei.setStart("2019-08-29 22:22:22");
        valid = mei.validate();
        assertEquals(false, valid);

        mei = new MonitoringExportInputs();
        mei.setClusterId("test");
        valid = mei.validate();
        assertEquals(true, valid);

        mei = new MonitoringExportInputs();
        mei.setClusterId("test");
        mei.setStart("2019-08-29 02:25");
        valid = mei.validate();
        assertEquals(true, valid);

        mei = new MonitoringExportInputs();
        mei.setClusterId("test");
        valid = mei.validate();
        assertEquals(true, valid);

        mei = new MonitoringExportInputs();
        mei.setClusterId("test");
        mei.setInterval(0);
        valid = mei.validate();
        assertEquals(false, valid);

        mei = new MonitoringExportInputs();
        mei.setClusterId("test");
        mei.setInterval(13);
        valid = mei.validate();
        assertEquals(false, valid);

        mei = new MonitoringExportInputs();
        mei.setClusterId("test");
        mei.setInterval(1);
        valid = mei.validate();
        assertEquals(true, valid);

        mei = new MonitoringExportInputs();
        mei.setClusterId("test");
        mei.setInterval(12);
        valid = mei.validate();
        assertEquals(true, valid);
    }

}
