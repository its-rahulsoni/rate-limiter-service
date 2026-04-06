package com.example.ratelimiter.spring.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RateLimiterRule {
    @NotNull(message = "keyType must not be null")
    private KeyType keyType;

    @NotNull(message = "limit must not be null")
    @Min(value = 1, message = "limit must be greater than 0")
    private Integer limit;

    @NotNull(message = "window must not be null")
    @Min(value = 1, message = "window must be greater than 0")
    private Integer window;

    private String algorithm;

    private String headerName;
    // --- Custom key extraction fields ---
    private String customParam; // Query parameter name
    private String customHeader; // Header name
    private String customJwtClaim; // JWT claim name (future extension)

    public KeyType getKeyType() {
        return keyType;
    }

    public void setKeyType(KeyType keyType) {
        this.keyType = keyType;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getWindow() {
        return window;
    }

    public void setWindow(Integer window) {
        this.window = window;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getCustomParam() {
        return customParam;
    }

    public void setCustomParam(String customParam) {
        this.customParam = customParam;
    }

    public String getCustomHeader() {
        return customHeader;
    }

    public void setCustomHeader(String customHeader) {
        this.customHeader = customHeader;
    }

    public String getCustomJwtClaim() {
        return customJwtClaim;
    }

    public void setCustomJwtClaim(String customJwtClaim) {
        this.customJwtClaim = customJwtClaim;
    }
}
