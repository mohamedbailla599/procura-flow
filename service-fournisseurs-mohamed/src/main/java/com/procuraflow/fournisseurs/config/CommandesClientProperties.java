package com.procuraflow.fournisseurs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.commandes")
public class CommandesClientProperties {

    /**
     * Base URL of Yassine's Commandes microservice (e.g. http://localhost:8082 or http://service-commandes:8082).
     */
    private String baseUrl = "http://localhost:8082";

    /**
     * HTTP connection timeout in milliseconds.
     */
    private int connectTimeoutMs = 3000;

    /**
     * HTTP read timeout in milliseconds.
     */
    private int readTimeoutMs = 5000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
