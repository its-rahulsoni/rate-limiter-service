package com.example.ratelimiter.spring.config;

import com.example.ratelimiter.spring.model.RateLimiterRule;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "rateLimiter")
public class RateLimiterProperties {
    private List<RateLimiterRule> rules;

    public List<RateLimiterRule> getRules() {
        return rules;
    }

    public void setRules(List<RateLimiterRule> rules) {
        this.rules = rules;
    }
}
