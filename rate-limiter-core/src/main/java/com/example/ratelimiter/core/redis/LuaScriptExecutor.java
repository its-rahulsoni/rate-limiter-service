package com.example.ratelimiter.core.redis;

import com.example.ratelimiter.common.models.RateLimitResult;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import io.lettuce.core.RedisCommandExecutionException;

/**
 * Flow:
 *
 * Request
 *    ↓
 * executeAndParseAsync()
 *    ↓
 * tryEvalSha()
 *    ↓
 *    ├── success → parse → return
 *    │
 *    └── failure →
 *           ├── NOSCRIPT → eval → reload → return
 *           ├── timeout → fallback
 *           └── other → fallback
 */

/**
 * Production-ready, non-blocking Lua script executor for Redis.
 * Lua script result contract: [allowed (int), remaining (long), retryAfter (long), capacity (long)]
 */
public class LuaScriptExecutor {
    private static final Logger logger = LoggerFactory.getLogger(LuaScriptExecutor.class);
    // Lua result indexes
    private static final int IDX_ALLOWED = 0;
    private static final int IDX_REMAINING = 1;
    private static final int IDX_RETRY_AFTER = 2;
    private static final int IDX_CAPACITY = 3;
    private final RedisAsyncCommands<String, String> redisAsyncCommands;
    private final String luaScript;
    private volatile String scriptSha;
    private final Duration timeout;
    // (Optional) Metrics hooks
    private final LongAdder successCount = new LongAdder();
    private final LongAdder fallbackCount = new LongAdder();
    private final LongAdder failureCount = new LongAdder();
    private final ReentrantLock scriptLoadLock = new ReentrantLock();

    public LuaScriptExecutor(RedisAsyncCommands<String, String> redisAsyncCommands, String luaScript, Duration timeout) {
        this.redisAsyncCommands = redisAsyncCommands;
        this.luaScript = luaScript;
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(3);
        loadScriptAsync();
    }

    private void loadScriptAsync() {
        boolean acquired = scriptLoadLock.tryLock();
        if (!acquired) {
            logger.debug("Script load already in progress, skipping duplicate reload.");
            return;
        }
        redisAsyncCommands.scriptLoad(luaScript)
            .toCompletableFuture()
            .whenComplete((sha, ex) -> {
                try {
                    if (ex == null) {
                        this.scriptSha = sha;
                        logger.info("Loaded Lua script with SHA: {}", sha);
                    } else {
                        logger.error("Failed to load Lua script", ex);
                    }
                } finally {
                    scriptLoadLock.unlock();
                }
            });
    }

    /**
     * Executes the cached Lua script asynchronously and parses the result into RateLimitResult.
     * Fallbacks to EVAL only if error is NOSCRIPT. Reloads script SHA if needed.
     * @param keys Redis keys
     * @param args Script arguments
     * @return CompletableFuture<RateLimitResult>
     */
    public CompletableFuture<RateLimitResult> executeAndParseAsync(List<String> keys, List<String> args) {
        if (keys == null || keys.isEmpty() || args == null || args.isEmpty()) {
            logger.error("LuaScriptExecutor: keys and args must not be null or empty. script={}, keys={}, args={}", scriptName(), keys, args);
            failureCount.increment();
            return CompletableFuture.completedFuture(RateLimitResult.fallback());
        }
        long start = System.nanoTime();
        String[] keysArr = keys.toArray(new String[0]);
        String[] argsArr = args.toArray(new String[0]);
        return tryEvalSha(keysArr, argsArr)
            .thenApply(result -> {
                successCount.increment();
                logLatency(start, "success");
                return parseResult(result);
            })
            .exceptionallyCompose(ex -> {
                if (isNoScriptError(ex)) {
                    fallbackCount.increment();
                    if (logger.isDebugEnabled()) {
                        logger.debug("NOSCRIPT: falling back to eval. script={}, keys={}, args={}", scriptName(), keys, args);
                    }
                    loadScriptAsync();
                    return tryEval(keysArr, argsArr)
                        .thenApply(result -> {
                            logLatency(start, "fallback");
                            return parseResult(result);
                        })
                        .exceptionally(fallbackEx -> {
                            failureCount.increment();
                            logger.error("Lua script eval fallback failed. script={}, keys={}, args={}, error= {}", scriptName(), keys, args, fallbackEx.toString());
                            logLatency(start, "fallback-failure");
                            return RateLimitResult.fallback();
                        });
                } else if (isTimeoutError(ex)) {
                    failureCount.increment();
                    logger.error("Lua script execution timed out. script={}, keys={}, args={}, error= {}", scriptName(), keys, args, ex.toString());
                    logLatency(start, "timeout");
                    return CompletableFuture.completedFuture(RateLimitResult.fallback());
                } else {
                    failureCount.increment();
                    logger.error("Lua script evalsha failed (not NOSCRIPT). script={}, keys={}, args={}, error= {}", scriptName(), keys, args, ex.toString());
                    logLatency(start, "failure");
                    return CompletableFuture.completedFuture(RateLimitResult.fallback());
                }
            })
            .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private String scriptName() {
        // Optionally extract a script name or hash for logging context
        return (luaScript != null && luaScript.length() > 32) ? luaScript.substring(0, 32) + "..." : luaScript;
    }

    private boolean isNoScriptError(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof RedisCommandExecutionException) {
                String msg = cause.getMessage();
                if (msg != null && msg.startsWith("NOSCRIPT")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean isTimeoutError(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void logLatency(long start, String label) {
        long elapsedMicros = (System.nanoTime() - start) / 1000;
        logger.debug("LuaScriptExecutor latency ({}): {} μs", label, elapsedMicros);
    }

    private CompletableFuture<Object> tryEvalSha(String[] keysArr, String[] argsArr) {
        String sha = this.scriptSha;
        if (sha == null) {
            logger.debug("Lua script SHA not loaded yet. Falling back to eval.");
            return tryEval(keysArr, argsArr);
        }
        return redisAsyncCommands.evalsha(sha, ScriptOutputType.MULTI, keysArr, argsArr)
            .toCompletableFuture();
    }

    private CompletableFuture<Object> tryEval(String[] keysArr, String[] argsArr) {
        return redisAsyncCommands.eval(luaScript, ScriptOutputType.MULTI, keysArr, argsArr)
            .toCompletableFuture();
    }

    private RateLimitResult parseResult(Object result) {
        if (result instanceof List<?> list && list.size() == 4) {
            try {
                boolean allowed = Integer.parseInt(Objects.toString(list.get(IDX_ALLOWED))) == 1;
                long remaining = Long.parseLong(Objects.toString(list.get(IDX_REMAINING)));
                long retryAfter = Long.parseLong(Objects.toString(list.get(IDX_RETRY_AFTER)));
                long capacity = Long.parseLong(Objects.toString(list.get(IDX_CAPACITY)));
                return new RateLimitResult(allowed, remaining, retryAfter, capacity);
            } catch (Exception e) {
                logger.error("Failed to parse Lua script result: {}", list, e);
            }
        } else {
            logger.error("Invalid Lua script result: {}", result);
        }
        return RateLimitResult.fallback();
    }

    // Optional: metrics accessors
    public long getSuccessCount() { return successCount.sum(); }
    public long getFallbackCount() { return fallbackCount.sum(); }
    public long getFailureCount() { return failureCount.sum(); }

    public String getScriptSha() {
        return scriptSha;
    }
}
