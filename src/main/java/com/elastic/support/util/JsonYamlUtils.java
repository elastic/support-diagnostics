/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package com.elastic.support.util;

import com.elastic.support.diagnostics.DiagnosticException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class JsonYamlUtils {

   private static final Logger logger = LoggerFactory.getLogger(JsonYamlUtils.class);

   public static ObjectMapper mapper = new ObjectMapper();

   public static JsonNode createJsonNodeFromFileName(String dir, String fileName) {
      File jsonFile = FileUtils.getFile(dir, fileName);

      try {
         String fileString = FileUtils.readFileToString(jsonFile, "UTF8");

         return JsonYamlUtils.createJsonNodeFromString(fileString);
      } catch (IOException e) {
         logger.info("Error reading in JSON string from file: {}", jsonFile);
         throw new RuntimeException(e);
      }
   }

   public static JsonNode createJsonNodeFromString(String nodeString) {
      try {
         return new ObjectMapper().readTree(nodeString);
      } catch (IOException e) {
         logger.info("Error creating JSON node from input string: {}", nodeString);
         throw new RuntimeException(e);
      }
   }

   public static Map<String, Object> readYamlFromClasspath(String path, boolean isBlock) throws DiagnosticException {
      try (
         InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
      ) {
         return JsonYamlUtils.readYaml(inputStream, isBlock);
      }
      catch (IOException e) {
         logger.info("Error reading YAML from {}", path);
         throw new DiagnosticException("Error reading YAML file",e);
      }
   }

   private static Map<String, Object> readYaml(InputStream in, boolean isBlock) {
      DumperOptions options = new DumperOptions();

      if (isBlock) {
         options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
      }

      Yaml yaml = new Yaml(options);
      Map<String, Object> doc = yaml.load(in);

      if (doc == null){
         return new HashMap<>();
      }

      return doc;
   }
}
