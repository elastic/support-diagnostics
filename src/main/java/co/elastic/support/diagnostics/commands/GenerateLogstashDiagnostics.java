/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.diagnostics.commands;

import co.elastic.support.Constants;
import co.elastic.support.diagnostics.DiagnosticException;
import co.elastic.support.diagnostics.chain.Command;
import co.elastic.support.diagnostics.chain.DiagnosticContext;
import co.elastic.support.rest.RestClient;
import co.elastic.support.rest.RestResult;
import co.elastic.support.util.SystemUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModelException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenerateLogstashDiagnostics implements Command {

    private static final Logger logger = LogManager.getLogger(GenerateLogstashDiagnostics.class);

    private static final String DIAGNOSTIC_TEMPLATES_PATH = "logstash-diagnostic-templates/";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void execute(DiagnosticContext context) throws DiagnosticException {
        try {
            final RestClient client = context.resourceCache.getRestClient(Constants.restInputHost);
            final Datasource datasource = new Datasource(new JsonData(getNodeInfo(client), getNodeStats(client)));
            final Configuration configuration = createTemplateConfiguration();

            for (String templateFileName : getAllTemplates()) {
                writeTemplateOutputFile(configuration, templateFileName, datasource, getTemplateOutputFilePath(context.tempDir, templateFileName));
            }
        } catch (Exception e) {
            logger.info(Constants.CONSOLE, "Unexpected error - bypassing some or all Logstash diagnostics calls. {}", Constants.CHECK_LOG);
        }
    }

    private void writeTemplateOutputFile(Configuration configuration, String templateFileName, Datasource datasource, Path output) throws DiagnosticException {
        try {
            final Template template = configuration.getTemplate(templateFileName);
            final StringWriter writer = new StringWriter();
            template.process(datasource, writer);
            SystemUtils.writeToFile(writer.toString(), output.toString());
        } catch (IOException | TemplateException e) {
            logger.error(Constants.CONSOLE, "Could not generate Logstash diagnostic file - skipping template {}.", templateFileName, e);
        }
    }

    private Path getTemplateOutputFilePath(String targetDir, String templateFileName) {
        final String outputFileName = templateFileName.substring(0, templateFileName.lastIndexOf(".ftlh"));
        return Paths.get(targetDir, "logstash_diagnostic_" + outputFileName);
    }

    private Configuration createTemplateConfiguration() throws TemplateModelException {
        try {
            Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
            configuration.setClassLoaderForTemplateLoading(Thread.currentThread().getContextClassLoader(), DIAGNOSTIC_TEMPLATES_PATH);
            configuration.setDefaultEncoding("UTF-8");
            configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            configuration.setLogTemplateExceptions(false);
            configuration.setWrapUncheckedExceptions(true);
            configuration.setFallbackOnNullLoopVariable(false);
            configuration.setSQLDateAndTimeTimeZone(TimeZone.getDefault());
            configuration.setSharedVariable("JSON", configuration.getObjectWrapper().wrap(MAPPER));
            return configuration;
        } catch (TemplateModelException e) {
            logger.error("Error creating the Logstash diagnostic templates configuration", e);
            throw e;
        }
    }

    private List<String> getAllTemplates() throws IOException {
        try (final Stream<Path> allFiles = Files.list(Paths.get(Thread.currentThread().getContextClassLoader().getResource(DIAGNOSTIC_TEMPLATES_PATH).getPath()))) {
            return allFiles
                    .filter(p -> p.getFileName().toString().endsWith(".ftlh"))
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error retrieving Logstash diagnostic templates files from config directory:  {}", DIAGNOSTIC_TEMPLATES_PATH, e);
            throw e;
        }
    }

    private Map<String, Object> getNodeInfo(final RestClient client) throws DiagnosticException, JsonProcessingException {
        final RestResult response = client.execQuery("/_node?graph=true");
        if (!response.isValid()) {
            throw new DiagnosticException(response.formatStatusMessage("Could not retrieve the Logstash node information - unable to continue."));
        }

        try {
            return MAPPER.readValue(response.toString(), new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            logger.error("Error retrieving the Logstash node information.", e);
            throw e;
        }
    }

    private Map<String, Object> getNodeStats(final RestClient client) throws DiagnosticException, JsonProcessingException {
        final RestResult response = client.execQuery("/_node/stats?vertices=true");
        if (!response.isValid()) {
            throw new DiagnosticException(response.formatStatusMessage("Could not retrieve the Logstash node stats - unable to continue."));
        }

        try {
            return MAPPER.readValue(response.toString(), new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            logger.error("Error retrieving the Logstash node stats.", e);
            throw e;
        }
    }

    public static class Datasource {
        public final JsonData data;

        public Datasource(JsonData data) {
            this.data = data;
        }

        public JsonData getData() {
            return data;
        }
    }

    public static class JsonData {
        private static final String VERSION = "0.0.1";
        private final ZonedDateTime createdDate = ZonedDateTime.now();
        private final Map<String, Object> info;
        private final Map<String, Object> stats;

        public JsonData(Map<String, Object> info, Map<String, Object> stats) {
            this.info = info;
            this.stats = stats;
        }

        public String getCreatedDate() {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(createdDate);
        }

        public String getVersion() {
            return VERSION;
        }

        public Map<String, Object> getInfo() {
            return info;
        }

        public Map<String, Object> getStats() {
            return stats;
        }
    }
}
