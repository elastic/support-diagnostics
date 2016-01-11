package com.elastic.support.diagnostics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class RestModule {

    RestTemplate restTemplate;
    HttpEntity<String> request;
    private final static Logger logger = LoggerFactory.getLogger(RestModule.class);

    public RestModule(RestTemplate restTemplate, HttpEntity<String> request) {
        this.restTemplate = restTemplate;
        this.request = request;
    }

    public String submitRequest(String url) {

        String result;
        try {;
            logger.debug("Submitting: " + url);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            result = response.getBody();
            logger.debug(result);

        } catch (RestClientException e) {
            if (url.contains("/license")) {
                return "no licenses installed";
            }
            String msg = "Please check log file for additional details.";
            logger.error("Error submitting request\n:", e);
            if (e.getMessage().contains("401 Unauthorized")) {
                msg = "Authentication failure: invalid login credentials.\n" + msg;
            }
            throw new RuntimeException(msg);
        }

        return result;

    }
}