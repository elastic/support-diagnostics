package com.elastic.support.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.*;


public class JsonYamlUtils {

    private static final Logger logger = LoggerFactory.getLogger(JsonYamlUtils.class);
    public static JsonNode createJsonNodeFromFileName(String fileName) throws Exception{

        File jsonFile = FileUtils.getFile(fileName);
        return createJsonNodeFromFile(jsonFile);

    }

    public static JsonNode createJsonNodeFromFileName(String dir, String fileName) throws Exception{

        File jsonFile = FileUtils.getFile(dir, fileName);
        return createJsonNodeFromFile(jsonFile);

    }

    public static JsonNode createJsonNodeFromFile(File jsonFile) throws Exception {

        String fileString = FileUtils.readFileToString(jsonFile);
        return JsonYamlUtils.createJsonNodeFromString(fileString);

    }

    public static JsonNode createJsonNodeFromString(String nodeString) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(nodeString);

    }

    public static JsonNode createJsonNodeFromClasspath(String path) throws Exception {

        InputStream is;
        is = JsonYamlUtils.class.getClassLoader().getResourceAsStream(path);
        String nodeString = new String(IOUtils.toByteArray(is));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(nodeString);

    }

   public static void writeYaml(String path, Map tree) throws Exception{

      Yaml yaml = new Yaml();
      FileWriter writer = new FileWriter(path);
      yaml.dump(tree, writer);
   }


    public static Map<String, Object> readYamlFromClasspath(String path, boolean isBlock) throws Exception {

        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);

        Map<String, Object> doc = JsonYamlUtils.readYaml(inputStream, isBlock);
        IOUtils.closeQuietly(inputStream);

        return doc;
    }

    public static Map<String, Object> readYamlFromPath(String path, boolean isBlock) throws Exception{
        File fl = FileUtils.getFile(path);
        InputStream inputStream = new FileInputStream(fl);
        Map<String, Object> doc = JsonYamlUtils.readYaml(inputStream, isBlock);
        IOUtils.closeQuietly(inputStream);

        return doc;
    }

    public static Map<String, Object> readYaml(InputStream in, boolean isBlock) throws Exception{

        DumperOptions options = new DumperOptions();
        if (isBlock) {
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        }

        Yaml yaml = new Yaml(options);
        Map<String, Object> map =  (Map<String, Object>)yaml.load(in);

        return map;
    }

    public static Map<String, Object> flattenYaml(Map<String, Object> map){
        Map<String, Object> result = new LinkedHashMap<>();
        buildFlattenedMap(result, map, null);
        return result;
    }

    public static Map<String, Object> flattenNode(JsonNode node) throws RuntimeException{
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonMap = mapper.convertValue(node, Map.class);
            //String json = mapper.writeValueAsString(node)
            //Map<String, Object> jsonMap = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> flat = flattenYaml(jsonMap);
            return flat;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, String path) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.hasText(path)) {
                if (key.startsWith("[")) {
                    key = path + key;
                }
                else {
                    key = path + "." + key;
                }
            }
            Object value = entry.getValue();
            if (value instanceof String) {
                result.put(key, value);
            }
            else if (value instanceof Map) {
                // Need a compound key
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;
                buildFlattenedMap(result, map, key);
            }
            else if(value instanceof List){
                result.put(key, value);
            }
            else if (value instanceof Collection) {
                // Need a compound key
                @SuppressWarnings("unchecked")
                Collection<Object> collection = (Collection<Object>) value;
                int count = 0;
                for (Object object : collection) {
                    buildFlattenedMap(result,
                            Collections.singletonMap("[" + (count++) + "]", object), key);
                }
            }
            else {
                result.put(key, value == null ? "" : value);
            }
        }
    }


}
