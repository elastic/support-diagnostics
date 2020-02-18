package com.elastic.support.diagnostics;

import com.elastic.support.Constants;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestDockerAwareness {

    @Test
    public void testDockerAwareness() {

        try {
            InputStream in = this.getClass().getClassLoader().getResourceAsStream("proc/1/cgroup-yes");
            List<String> entries = IOUtils.readLines(in, Constants.UTF_8);
            assertTrue(SystemUtils.checkCGroupEntries(entries));

            in = this.getClass().getClassLoader().getResourceAsStream("proc/1/cgroup-yes");
            entries = IOUtils.readLines(in, Constants.UTF_8);
            assertFalse(SystemUtils.checkCGroupEntries(entries));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
