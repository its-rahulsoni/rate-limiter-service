package com.example.ratelimiter.core.api;

import com.example.ratelimiter.common.models.RateLimitResult;
import com.example.ratelimiter.common.models.RateLimiterRule;

/**
 * Core interface for rate limiting.
 * Extensible for multiple algorithms and storage backends.
 */
public interface RateLimiter {
    /**
     * Checks if a request is allowed for the given key and rule.
     * @param key Unique identifier (IP, user, etc.)
     * @param rule Rate limiting rule
     * @return RateLimitResult (allowed, remaining, retryAfter, etc.)
     */
    RateLimitResult allow(String key, RateLimiterRule rule);
}
