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

        MonitoringExportInputs mei = new MonitoringExportInputs();

        // Check cluster id validation
        boolean valid = mei.validate();
        assertEquals(false, valid);

        mei.setClusterId("test");
        valid = mei.validate();
        assertEquals(true, valid);

        mei.setStartDate("08-29-2019");
        valid = mei.validate();
        assertEquals(false, valid);

        mei.setStartDate("2019-08-29");
        valid = mei.validate();
        assertEquals(true, valid);

        mei.setStartTime("2:25:2");
        valid = mei.validate();
        assertEquals(false, valid);

        mei.setStartTime("02:25");
        valid = mei.validate();
        assertEquals(true, valid);

        mei.setInterval(0);
        valid = mei.validate();
        assertEquals(false, valid);

        mei.setInterval(13);
        valid = mei.validate();
        assertEquals(false, valid);

        mei.setInterval(1);
        valid = mei.validate();
        assertEquals(true, valid);

        mei.setInterval(12);
        valid = mei.validate();
        assertEquals(true, valid);
    }

}
