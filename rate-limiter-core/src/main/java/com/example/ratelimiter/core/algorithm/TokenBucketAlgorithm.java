package com.example.ratelimiter.core.algorithm;

import com.example.ratelimiter.common.models.RateLimiterRule;
import com.example.ratelimiter.common.models.RateLimitResult;
import com.example.ratelimiter.core.redis.LuaScriptExecutor;
import com.example.ratelimiter.core.util.KeyBuilder;
import io.lettuce.core.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;

/**
 * Token Bucket algorithm implementation using Redis and Lua.
 *
 * Lua script argument contract (order):
 *   1. capacity (tokens, integer)
 *   2. refill_rate (tokens per second, float)
 *   3. now (current timestamp in seconds, long)
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
        double refillRate = (double) capacity / (double) windowSeconds; // tokens per second, floating point
        long nowSeconds = clock.millis() / 1000L; // current time in seconds
        // All time units in this algorithm are in seconds
        // Use BigDecimal to avoid scientific notation when passing refillRate to Lua
        // Scientific notation (e.g., 1.23E-4) is unsafe for Lua tonumber() parsing on some Redis/Lua versions
        String refillRateStr = BigDecimal.valueOf(refillRate).toPlainString();
        List<String> args = List.of(
                String.valueOf(capacity), // capacity: integer (tokens)
                refillRateStr,            // refill_rate: tokens per second (float)
                String.valueOf(nowSeconds) // now: timestamp in seconds (long)
        );
        List<String> keys = Collections.singletonList(redisKey);
        return luaScriptExecutor.executeAndParseAsync(keys, args)
            .thenApply(result -> {
                recordLatency(start);
                RateLimitResult safeResult = parseLuaResult(result, key, rule, args, redisKey);
                if (safeResult == null) {
                    failureCount.increment();
                    logger.error("[RateLimiter] Invalid Lua result. key={}, redisKey={}, rule={}, args={}, rawResponse={}", key, redisKey, rule, args, result);
                    return fallbackResult(rule);
                }
                if (safeResult.isAllowed()) {
                    successCount.increment();
                    logger.info("[RateLimiter] ALLOWED key={} redisKey={} rule={} tokensLeft={} retryAfter={}", key, redisKey, rule, safeResult.getRemaining(), safeResult.getRetryAfter());
                } else {
                    blockedCount.increment();
                    logger.info("[RateLimiter] REJECTED key={} redisKey={} rule={} tokensLeft={} retryAfter={}", key, redisKey, rule, safeResult.getRemaining(), safeResult.getRetryAfter());
                }
                return safeResult;
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

    /**
     * Robustly parses the Lua script result, ensuring type safety and defensive checks.
     * If validation fails, logs error and returns null.
     */
    @SuppressWarnings("unchecked")
    private RateLimitResult parseLuaResult(Object result, String key, RateLimiterRule rule, List<String> args, String redisKey) {
        if (result == null) {
            logger.error("[RateLimiter] Lua result is null. key={}, redisKey={}, rule={}, args={}", key, redisKey, rule, args);
            return null;
        }
        if (!(result instanceof List<?>)) {
            logger.error("[RateLimiter] Lua result is not a List. key={}, redisKey={}, rule={}, args={}, result={}", key, redisKey, rule, args, result);
            return null;
        }
        List<?> list = (List<?>) result;
        if (list.size() != 5) {
            logger.error("[RateLimiter] Lua result does not have 5 elements. key={}, redisKey={}, rule={}, args={}, result={}", key, redisKey, rule, args, list);
            return null;
        }
        try {
            Integer allowed = toInt(list.get(0));
            Integer tokens = toInt(list.get(1));
            Integer retryAfter = toInt(list.get(2));
            Integer capacity = toInt(list.get(3));
            Long now = toLong(list.get(4));
            if (allowed == null || tokens == null || retryAfter == null || capacity == null || now == null) {
                logger.error("[RateLimiter] Lua result contains null or invalid types. key={}, redisKey={}, rule={}, args={}, result={}", key, redisKey, rule, args, list);
                return null;
            }
            return new RateLimitResult(allowed == 1, tokens, retryAfter, capacity);
        } catch (Exception e) {
            logger.error("[RateLimiter] Exception parsing Lua result. key={}, redisKey={}, rule={}, args={}, result={}", key, redisKey, rule, args, list, e);
            return null;
        }
    }

    private Integer toInt(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Long) return ((Long) obj).intValue();
        if (obj instanceof String) {
            try { return Integer.parseInt((String) obj); } catch (NumberFormatException ignored) {}
        }
        if (obj instanceof Number) return ((Number) obj).intValue();
        return null;
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof String) {
            try { return Long.parseLong((String) obj); } catch (NumberFormatException ignored) {}
        }
        if (obj instanceof Number) return ((Number) obj).longValue();
        return null;
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
