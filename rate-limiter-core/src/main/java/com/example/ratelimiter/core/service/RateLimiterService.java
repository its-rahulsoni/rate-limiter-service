package com.example.ratelimiter.core.service;

import com.example.ratelimiter.common.enums.AlgorithmType;
import com.example.ratelimiter.common.models.RateLimiterRule;
import com.example.ratelimiter.common.models.RateLimitResult;
import com.example.ratelimiter.core.algorithm.RateLimitingAlgorithm;
import com.example.ratelimiter.core.factory.AlgorithmFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Service to delegate rate limiting to the correct algorithm.
 */
public class RateLimiterService {
    private final AlgorithmFactory algorithmFactory;

    public RateLimiterService(AlgorithmFactory algorithmFactory) {
        this.algorithmFactory = algorithmFactory;
    }

    /**
     * Accepts a key and rule, selects the algorithm, and delegates execution.
     * @param key Unique identifier (IP, user, etc.)
     * @param rule Rate limiting rule
     * @return CompletableFuture<RateLimitResult>
     */
    public CompletableFuture<RateLimitResult> allow(String key, RateLimiterRule rule) {
        AlgorithmType type = AlgorithmType.valueOf(rule.getAlgorithm());
        RateLimitingAlgorithm algorithm = algorithmFactory.getAlgorithm(type);
        return algorithm.allow(key, rule);
    }
}
