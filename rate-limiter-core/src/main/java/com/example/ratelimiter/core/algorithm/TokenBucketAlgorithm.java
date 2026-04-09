package com.example.ratelimiter.core.algorithm;

import com.example.ratelimiter.common.models.RateLimiterRule;
import com.example.ratelimiter.common.models.RateLimitResult;
import com.example.ratelimiter.core.redis.LuaScriptExecutor;
import com.example.ratelimiter.core.util.KeyBuilder;
import io.lettuce.core.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;

/**
 * Token Bucket algorithm implementation using Redis and Lua.
 *
 * Lua script argument contract (order):
 *   1. capacity (tokens)
 *   2. windowMs (window in milliseconds)
 *   3. nowMs (current time in milliseconds)
 *
 * Flow:
 *
 * HTTP Request
 *    ↓
 * Interceptor / Aspect
 *    ↓
 * TokenBucketAlgorithm.allow()
 *    ↓
 * Prepare key + args
 *    ↓
 * LuaScriptExecutor
 *    ↓
 * Redis (Lua execution)
 *    ↓
 * Result returned
 *    ↓
 * Metrics updated
 *    ↓
 * Response sent
 */
public class TokenBucketAlgorithm implements RateLimitingAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(TokenBucketAlgorithm.class);
    private final KeyBuilder keyBuilder;
    private final LuaScriptExecutor luaScriptExecutor;
    private final Clock clock;
    private final boolean failOpen; // true = allow on error, false = block on error
    private final String redisKeyPrefix;
    // --- Metrics ---
    /**
     * successCount: Number of requests allowed (not rate-limited).
     * blockedCount: Number of requests blocked due to rate limiting.
     * failureCount: Number of requests failed due to system/infra errors (e.g. Redis errors, logic errors).
     * timeoutCount: Number of requests that failed due to timeout.
     * totalRequests: Total number of requests processed (sum of all outcomes).
     * avgLatencyMicros: Average latency (in microseconds) for all requests (success, blocked, failure, timeout).
     *
     * These metrics are intended for production monitoring and alerting:
     * - successCount/blockedCount reflect business-level rate limiting effectiveness.
     * - failureCount/timeoutCount indicate system health and reliability.
     * - totalRequests/avgLatencyMicros provide throughput and performance insights.
     */
    private final LongAdder successCount = new LongAdder();
    private final LongAdder failureCount = new LongAdder();
    private final LongAdder timeoutCount = new LongAdder();
    private final LongAdder blockedCount = new LongAdder();
    private final LongAdder totalLatencyMicros = new LongAdder();
    private final LongAdder totalRequests = new LongAdder();

    public TokenBucketAlgorithm(KeyBuilder keyBuilder, LuaScriptExecutor luaScriptExecutor, Clock clock, boolean failOpen, String redisKeyPrefix) {
        this.keyBuilder = keyBuilder;
        this.luaScriptExecutor = luaScriptExecutor;
        this.clock = clock;
        this.failOpen = failOpen;
        this.redisKeyPrefix = redisKeyPrefix != null ? redisKeyPrefix : "rate_limit:token_bucket:";
    }

    @Override
    public CompletableFuture<RateLimitResult> allow(String key, RateLimiterRule rule) {
        long start = System.nanoTime();
        totalRequests.increment();
        if (key == null || key.isEmpty() || rule == null || rule.getLimit() <= 0 || rule.getWindow() <= 0) {
            logger.warn("TokenBucketAlgorithm: Invalid input. key={}, rule={}, algorithm=TOKEN_BUCKET", key, rule);
            failureCount.increment();
            return CompletableFuture.completedFuture(fallbackResult(rule));
        }
        String redisKey = redisKeyPrefix + keyBuilder.build(rule.getAlgorithm(), key);
        int capacity = rule.getLimit();
        int windowSeconds = rule.getWindow(); // window in seconds
        int refillRate = capacity / windowSeconds; // tokens per second
        long nowSeconds = clock.millis() / 1000L; // current time in seconds
        // All time units in this algorithm are in seconds
        List<String> args = List.of(
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(nowSeconds)
        );
        List<String> keys = Collections.singletonList(redisKey);
        return luaScriptExecutor.executeAndParseAsync(keys, args)
            .thenApply(result -> {
                recordLatency(start);
                if (result == null) {
                    failureCount.increment();
                    logger.warn("TokenBucketAlgorithm: Null result from Lua. key={}, rule={}, algorithm=TOKEN_BUCKET", key, rule);
                    return fallbackResult(rule);
                }
                if (result.isAllowed()) {
                    successCount.increment();
                } else {
                    blockedCount.increment();
                }
                return result;
            })
            .exceptionally(ex -> {
                recordLatency(start);
                Throwable root = unwrapCompletionException(ex);
                if (isTimeout(root)) {
                    timeoutCount.increment();
                    logger.warn("TokenBucketAlgorithm: Timeout. key={}, rule={}, algorithm=TOKEN_BUCKET", key, rule, root);
                } else if (isRedisError(root)) {
                    failureCount.increment();
                    logger.error("TokenBucketAlgorithm: Redis error. key={}, rule={}, algorithm=TOKEN_BUCKET", key, rule, root);
                } else {
                    failureCount.increment();
                    logger.error("TokenBucketAlgorithm: Logic/other error. key={}, rule={}, algorithm=TOKEN_BUCKET", key, rule, root);
                }
                return fallbackResult(rule);
            });
    }

    private RateLimitResult fallbackResult(RateLimiterRule rule) {
        // Fail-open: allow all, Fail-closed: block all
        if (failOpen) {
            return new RateLimitResult(true, 0, 0, rule != null ? rule.getLimit() : 0);
        } else {
            return new RateLimitResult(false, 0, 1, rule != null ? rule.getLimit() : 0);
        }
    }

    private void recordLatency(long start) {
        long elapsed = (System.nanoTime() - start) / 1000;
        totalLatencyMicros.add(elapsed);
    }

    private Throwable unwrapCompletionException(Throwable ex) {
        Throwable cause = ex;
        while (cause instanceof java.util.concurrent.CompletionException || cause instanceof java.util.concurrent.ExecutionException) {
            cause = cause.getCause();
        }
        return cause != null ? cause : ex;
    }

    private boolean isTimeout(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof java.util.concurrent.TimeoutException) return true;
            cause = cause.getCause();
        }
        return false;
    }

    private boolean isRedisError(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof RedisException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    // Metrics accessors
    public long getSuccessCount() { return successCount.sum(); }
    public long getFailureCount() { return failureCount.sum(); }
    public long getTimeoutCount() { return timeoutCount.sum(); }
    public long getBlockedCount() { return blockedCount.sum(); }
    public long getTotalRequests() { return totalRequests.sum(); }
    public double getAvgLatencyMicros() {
        long req = totalRequests.sum();
        return req == 0 ? 0 : (double) totalLatencyMicros.sum() / req;
    }
}
