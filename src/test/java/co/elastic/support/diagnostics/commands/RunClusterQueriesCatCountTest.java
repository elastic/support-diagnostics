/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.rest.RestEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunClusterQueriesCatCountTest {

    private static final RestEntry COUNT = new RestEntry("count", "", ".json", false, "/_count", true);
    private static final RestEntry CAT_COUNT = new RestEntry("cat_count", "cat", ".txt", false, "/_cat/count", true);

    private DiagnosticContext context(Path dir, RestEntry... entries) {
        DiagnosticContext context = new DiagnosticContext(null, null, null, false);
        context.tempDir = dir.toString();
        Map<String, RestEntry> calls = new LinkedHashMap<>();
        for (RestEntry entry : entries) {
            calls.put(entry.getName(), entry);
        }
        context.elasticRestCalls = calls;
        return context;
    }

    @Test
    void writesCatFormat_fromCountJson(@TempDir Path dir) throws Exception {
        Path countFile = dir.resolve("count.json");
        Files.writeString(countFile, "{\"count\":12345,\"_shards\":{\"total\":3,\"successful\":3,\"skipped\":0,\"failed\":0}}");
        Files.setLastModifiedTime(countFile, FileTime.from(Instant.parse("2026-11-10T14:43:05Z")));

        new RunClusterQueries().deriveCatCount(context(dir, COUNT));

        assertEquals("1794321785 14:43:05 12345\n", Files.readString(dir.resolve("cat").resolve("cat_count.txt")));
    }

    @Test
    void handlesCountsBeyondIntRange(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("count.json"), "{\"count\":9876543210123}");

        new RunClusterQueries().deriveCatCount(context(dir, COUNT));

        String line = Files.readString(dir.resolve("cat").resolve("cat_count.txt"));
        assertTrue(line.matches("\\d+ \\d{2}:\\d{2}:\\d{2} 9876543210123\n"), line);
    }

    @Test
    void skips_whenCountJsonIsAnErrorBody(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("count.json"), "{\"error\":{\"type\":\"security_exception\"},\"status\":403}");

        new RunClusterQueries().deriveCatCount(context(dir, COUNT));

        assertFalse(Files.exists(dir.resolve("cat").resolve("cat_count.txt")));
    }

    @Test
    void skips_whenCountJsonIsNotJson(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("count.json"), "<html>Bad Gateway</html>");

        new RunClusterQueries().deriveCatCount(context(dir, COUNT));

        assertFalse(Files.exists(dir.resolve("cat").resolve("cat_count.txt")));
    }

    @Test
    void skips_whenCountJsonIsMissing(@TempDir Path dir) {
        new RunClusterQueries().deriveCatCount(context(dir, COUNT));

        assertFalse(Files.exists(dir.resolve("cat").resolve("cat_count.txt")));
    }

    @Test
    void keepsCollectedCatCount_whenCatCountWasCalledDirectly(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("count.json"), "{\"count\":1}");
        Files.createDirectories(dir.resolve("cat"));
        Files.writeString(dir.resolve("cat").resolve("cat_count.txt"), "original\n");

        new RunClusterQueries().deriveCatCount(context(dir, COUNT, CAT_COUNT));

        assertEquals("original\n", Files.readString(dir.resolve("cat").resolve("cat_count.txt")));
    }

    @Test
    void skips_whenCountIsNotConfigured(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("count.json"), "{\"count\":1}");

        new RunClusterQueries().deriveCatCount(context(dir));

        assertFalse(Files.exists(dir.resolve("cat").resolve("cat_count.txt")));
    }
}
