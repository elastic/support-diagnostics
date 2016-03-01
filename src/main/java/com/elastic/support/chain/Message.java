package com.elastic.support.chain;

import com.elastic.support.util.SystemUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class Message {

    protected String text = "";
    protected String category = "";
    protected String id = "";
    protected Map<String, Object> values = new LinkedHashMap<>();

    public Message() {
    }

    public Message(String text) {
        this.text = text;
    }

    public Message(String text, Map<String, Object> values) {
        this(text);
        if (values != null) this.values = values;
    }

    public Message(String id, String text, Map<String, Object> values) {
        this(text, values);
        this.id = id;
    }

    public Message(String category, String id, String text, Map<String, Object> values) {
        this(id, text, values);
        this.category = category;
    }

    public String getCategory() {
        return SystemUtils.safeToString(this.category);
    }

    public String getId() {
        return SystemUtils.safeToString(this.id);
    }

    public String getText() {
        return SystemUtils.safeToString(this.text);
    }

    public Map<String, Object> getValues() {
        return this.values;
    }

    public String asJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error encountered converting Message to Json String.");
        }
    }

    public Map toMap(){
        Map map = new LinkedHashMap<>();
        map.put("text", this.text);
        map.put("id", this.id);
        map.put("category", this.category);
        map.put("values", this.values);
        return map;
    }

    @Override
    public String toString() {
        return this.getText();
    }


}