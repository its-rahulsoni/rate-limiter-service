package com.example.ratelimiter.core.algorithm;

import com.example.ratelimiter.common.models.RateLimiterRule;
import com.example.ratelimiter.common.models.RateLimitResult;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for pluggable rate limiting algorithms.
 */
public interface RateLimitingAlgorithm {
    /**
     * Executes the rate limiting logic for the given key and rule.
     * @param key Unique identifier (IP, user, etc.)
     * @param rule Rate limiting rule
     * @return CompletableFuture<RateLimitResult> (allowed, remaining, retryAfter, etc.)
     */
    CompletableFuture<RateLimitResult> allow(String key, RateLimiterRule rule);
}
