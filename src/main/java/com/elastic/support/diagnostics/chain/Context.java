package com.elastic.support.diagnostics.chain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Context {

    Logger logger = LoggerFactory.getLogger(Context.class);

    public static final String CONTEXT_MSGS = "context";

    protected Map<String, Object> attributes = new LinkedHashMap<>();
    protected Map<String, List> messages = new LinkedHashMap<>();

    public Map<String, Object> getAttributes() {
        return attributes;
    }
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Object getAttribute(String key){
        return attributes.get(key);
    }

    public void addMessage(String msg){
        List ctxMsgs = messages.get(CONTEXT_MSGS);
        ctxMsgs.add(new Message(msg));
    }

    public List<String> getContextMessages() {
        return messages.get(CONTEXT_MSGS);
    }

    public Map<String, List> getMessages() { return messages; }

    public List getMessages(String key) {
        return messages.get(key);
    }

    public void setMessages(List msgList) {
        this.messages.put(CONTEXT_MSGS, msgList);
    }

    public void setMessages(String key, List msgList) {
        this.messages.put(key, msgList);
    }
    public Context(){
        messages.put(CONTEXT_MSGS, new ArrayList<String>());
    }

    public String getStringAttribute(String key){
        String ret = getTypedAttribute(key, String.class);
        return ret == null ? "" : ret;
    }

    public Long getLongAttribute(String key){
        Long ret = getTypedAttribute(key, Long.class);
        return ret == null ? new Long("0") : ret;
    }

    public Integer getIntegerAttribute(String key){
        Integer ret = getTypedAttribute(key, Integer.class);
        return ret == null ? new Integer("0") : ret;
    }

    public Map<String, Object> getMappedAttribute(String key){
        Map<String, Object> ret = getTypedAttribute(key, Map.class);;
        Object obj = attributes.get(key);
        return ret == null ? new LinkedHashMap<String, Object>() : ret;
    }

    public List<Object> getListAttribute(String key){
        List<Object> ret = getTypedAttribute(key, List.class);;
        Object obj = attributes.get(key);
        return ret == null ? new ArrayList<Object>() : ret;
    }

    public boolean setAttribute(String key, Object value){
        // Send back true if we replaced an existing attribute and false if it was new
        Object ret = attributes.get(key);
        attributes.put(key, value);
        return ret == null ? false : true;
    }

    public <T> T getTypedAttribute(String key, Class<T> clazz){

        Object val = attributes.get(key);
        T castValue;

        if(val == null){
            logger.warn("Attribute: " + key + "cannot be converted since it is null");
            return null;
        }

        try {
            castValue = clazz.cast(val);
        }
        catch (Exception e){
            logger.error("Attribute: " + key + " could not be cast to " + clazz, e);
            throw e;
        }

        return castValue;
    }

    @Override
    public String toString() {

        return attributes.toString();

    }
}
