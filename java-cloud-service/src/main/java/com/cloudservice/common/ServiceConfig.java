package com.cloudservice.common;

import java.util.HashMap;
import java.util.Map;

public class ServiceConfig {
    private String region;
    private String endpoint;
    private Map<String, String> credentials;
    private Map<String, Object> additionalProperties;

    public ServiceConfig() {
        this.credentials = new HashMap<>();
        this.additionalProperties = new HashMap<>();
        this.region = "us-east-1";
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, String> credentials) {
        this.credentials = credentials;
    }

    public void addCredential(String key, String value) {
        this.credentials.put(key, value);
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public void addProperty(String key, Object value) {
        this.additionalProperties.put(key, value);
    }

    public Object getProperty(String key) {
        return this.additionalProperties.get(key);
    }
}
