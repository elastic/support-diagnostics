package com.elastic.support.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


public class JsonYamlUtils {

   private static final Logger logger = LoggerFactory.getLogger(JsonYamlUtils.class);

   public static JsonNode createJsonNodeFromFileName(String fileName) {
      File jsonFile = FileUtils.getFile(fileName);
      return createJsonNodeFromFile(jsonFile);
   }

   public static JsonNode createJsonNodeFromFileName(String dir, String fileName) {
      File jsonFile = FileUtils.getFile(dir, fileName);
      return createJsonNodeFromFile(jsonFile);
   }

   public static JsonNode createJsonNodeFromFile(File jsonFile)  {
      try {
         String fileString = FileUtils.readFileToString(jsonFile, "UTF8");
         return JsonYamlUtils.createJsonNodeFromString(fileString);
      } catch (IOException e) {
         logger.error("Error reading in JSON string from file: {}", jsonFile);
         throw new RuntimeException(e);
      }
   }

   public static JsonNode createJsonNodeFromString(String nodeString) {
      try {
         ObjectMapper mapper = new ObjectMapper();
         return mapper.readTree(nodeString);
      } catch (IOException e) {
         logger.error("Error creating JSON node from input string: {}", nodeString);
         throw new RuntimeException(e);
      }
   }

   public static JsonNode createJsonNodeFromClasspath(String path) {
      try {
         InputStream is;
         is = JsonYamlUtils.class.getClassLoader().getResourceAsStream(path);
         String nodeString = new String(IOUtils.toByteArray(is));
         ObjectMapper mapper = new ObjectMapper();
         return mapper.readTree(nodeString);
      } catch (IOException e) {
         logger.error("Error creating JSON node {}", path);
         throw new RuntimeException(e);
      }
   }

   public static void writeYaml(String path, Map tree) {
      try {
         DumperOptions options = new DumperOptions();
         options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
         Yaml yaml = new Yaml(options);
         FileWriter writer = new FileWriter(path);
         yaml.dump(tree, writer);
      } catch (IOException e) {
         logger.error("Error writing YAML to: {}", path);
         throw new RuntimeException(e);
      }
   }

   public static Map readYamlFromClasspath(String path, boolean isBlock)  {
      try {
         InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
         Map doc = JsonYamlUtils.readYaml(inputStream, isBlock);
         SystemUtils.streamClose(path, inputStream);
         return doc;
      } catch (Exception e) {
         logger.error("Error reading YAML from {}", path);
         throw new RuntimeException(e);
      }
   }

   public static Map readYamlFromPath(String path, boolean isBlock) throws Exception {
      File fl = FileUtils.getFile(path);
      InputStream inputStream = new FileInputStream(fl);
      Map doc = JsonYamlUtils.readYaml(inputStream, isBlock);
      SystemUtils.streamClose(path, inputStream);
      return doc;
   }

   public static Map readYaml(InputStream in, boolean isBlock) throws Exception {
      Map doc = null;

      try {
         DumperOptions options = new DumperOptions();
         if (isBlock) {
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
         }

         Yaml yaml = new Yaml(options);
         doc = (Map) yaml.load(in);

      } catch (Exception e) {
         logger.error("Error encountered retrieving yml file.", e);
      }
      finally {
         return nullSafeYamlMap(doc);
      }
   }

   public static Map flattenYaml(Map map) {
      Map result = new LinkedHashMap<>();
      buildFlattenedMap(result, map, null);
      return result;
   }

   public static Map flattenMap(Map map){
      return flattenYaml(map);
   }

   public static Map flattenNode(JsonNode node) {
      try {
         ObjectMapper mapper = new ObjectMapper();
         Map jsonMap = mapper.convertValue(node, Map.class);
         //String json = mapper.writeValueAsString(node)
         //Map jsonMap = mapper.readValue(json, new TypeReference<Map>() {});
         Map flat = flattenYaml(jsonMap);
         return flat;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public static void buildFlattenedMap(Map result, Map source, String path) {

      Set<Map.Entry<Object, Object>> entries = source.entrySet();
      for (Map.Entry<Object, Object> entry : entries) {
         String key = entry.getKey().toString();
         if (StringUtils.isNoneEmpty(path)) {
            if (key.startsWith("[")) {
               key = path + key;
            } else {
               key = path + "." + key;
            }
         }
         Object value = entry.getValue();
         if (value instanceof String) {
            result.put(key, value);
         } else if (value instanceof Map) {
            // Need a compound key
            Map map = (Map) value;
            buildFlattenedMap(result, map, key);
         //} else if (value instanceof List) {
         //   result.put(key, value);
         } else if (value instanceof Collection) {
            // Need a compound key
            @SuppressWarnings("unchecked")
            Collection<Object> collection = (Collection<Object>) value;
            int count = 0;
            for (Object object : collection) {
               buildFlattenedMap(result,
                  Collections.singletonMap("[" + (count++) + "]", object), key);
            }
         } else {
            result.put(key, value == null ? "" : value);
         }
      }
   }

   private static Map nullSafeYamlMap(Map doc){
      if (doc == null){
         doc = new HashMap();
      }
      return doc;
   }

   private static Map listToMap(List input){
      int sz = input.size();
      Map output = new LinkedHashMap<>();
      for(int i=0; i < sz; i++){
         output.put("idx_" + i, input.get(i));
      }

      return output;

   }
}
