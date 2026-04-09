package com.example.ratelimiter.spring.config;

import com.example.ratelimiter.common.models.RateLimiterRule;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {
    private String mode;
    private Map<String, RateLimiterRule> rules;
    private List<RateLimiterRule> globalRules;

    public String getMode() {
        return mode;
    }
    public void setMode(String mode) {
        this.mode = mode;
    }
    public Map<String, RateLimiterRule> getRules() {
        return rules;
    }
    public void setRules(Map<String, RateLimiterRule> rules) {
        this.rules = rules;
    }
    public List<RateLimiterRule> getGlobalRules() {
        return globalRules;
    }
    public void setGlobalRules(List<RateLimiterRule> globalRules) {
        this.globalRules = globalRules;
    }
}
