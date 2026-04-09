package com.example.ratelimiter.core.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "rate-limiter.redis")
@Validated
public class RedisProperties {
    @NotBlank
    private String url;

    @NotNull
    @Positive
    private Duration timeout;

    @NotNull
    @Positive
    private Duration commandTimeout;

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public Duration getTimeout() {
        return timeout;
    }
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
    public Duration getCommandTimeout() {
        return commandTimeout;
    }
    public void setCommandTimeout(Duration commandTimeout) {
        this.commandTimeout = commandTimeout;
    }
}
