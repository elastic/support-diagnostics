package com.elastic.support.version;

import com.elastic.support.util.JsonYamlUtils;
import com.vdurmont.semver4j.Semver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class TestRestConfigFileValidity {

    private static final Logger logger = LogManager.getLogger(TestRestConfigFileValidity.class);

    protected static Semver sem= new Semver("9.9.999", Semver.SemverType.NPM);

    @Test
    public void validateElasticConfigVersioning(){
        Map<String, Object> restEntriesConfig = JsonYamlUtils.readYamlFromClasspath("elastic-rest.yml", true);
        validateEntries(restEntriesConfig);
        restEntriesConfig = JsonYamlUtils.readYamlFromClasspath("logstash-rest.yml", true);
        validateEntries(restEntriesConfig);
    }


    private void validateEntries(Map<String, Object> config) {
        for( Map.Entry<String, Object>entry : config.entrySet() ) {

            Map<String, Object> values = (Map) entry.getValue();
            Map<String, String> urls = (Map) values.get("versions");

            assertNotNull(values.get("subdir"));
            String ext = (String) values.get("extension");
            assertTrue(".txt".equals(ext) || ".json".equals(ext));
            assertNotNull(values.get("retry"));

            int nbrValid = 0;

            // For each entry there should be only 1 valid url.
            for (Map.Entry<String, String> url : urls.entrySet()) {
                if (sem.satisfies(url.getKey())) {
                    nbrValid++;
                }
            }

            assertTrue( nbrValid == 1 );

        }

    }

}
