package com.example.ratelimiter.spring.limiter;

import com.example.ratelimiter.spring.model.RateLimiterRule;

public interface RateLimiter {
    /**
     * Checks if a request is allowed for the given key and rule.
     * @param key the resolved key (IP, header value, etc.)
     * @param rule the rate limiter rule
     * @return true if allowed, false if rate limited
     */
    boolean allowRequest(String key, RateLimiterRule rule);
}
