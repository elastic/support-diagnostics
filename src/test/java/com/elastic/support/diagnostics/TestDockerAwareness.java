package com.elastic.support.diagnostics;

import com.elastic.support.Constants;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

public class TestDockerAwareness {

    @Test
    public void testDockerAwareness() {

        JsonNode node = JsonYamlUtils.createJsonNodeFromString("{ }");
        System.out.println(node.isEmpty());

    }
}
