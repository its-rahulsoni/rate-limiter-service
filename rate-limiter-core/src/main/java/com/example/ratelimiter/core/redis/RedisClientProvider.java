package com.example.ratelimiter.core.redis;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe Redis client provider for Lettuce.
 * Ensures connection reuse, proper lifecycle, and reconnection handling.
 * Do not expose raw RedisCommands; provide only safe, managed access.
 *
 * This class is intended to be managed as a singleton bean by Spring.
 */
public class RedisClientProvider {
    private static final Logger logger = LoggerFactory.getLogger(RedisClientProvider.class);
    private final ReentrantLock connectionLock = new ReentrantLock();

    private final RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;
    private final String redisUrl;
    private final Duration connectionTimeout;
    private final Duration commandTimeout;

    public RedisClientProvider(String redisUrl, Duration connectionTimeout, Duration commandTimeout) {
        this.redisUrl = redisUrl;
        this.connectionTimeout = connectionTimeout != null ? connectionTimeout : Duration.ofSeconds(5);
        this.commandTimeout = commandTimeout != null ? commandTimeout : Duration.ofSeconds(5);
        RedisURI redisURI = RedisURI.create(redisUrl);
        redisURI.setTimeout(this.connectionTimeout);
        this.redisClient = RedisClient.create(redisURI);
        this.redisClient.setOptions(ClientOptions.builder()
                .timeoutOptions(TimeoutOptions.enabled(this.commandTimeout))
                .build());
        this.connection = createConnectionWithRetry();
    }

    /**
     * Provides a managed, thread-safe RedisAsyncCommands instance.
     * Uses Lettuce's async API. Does not block threads.
     * Handles reconnection if needed, using double-check locking for connection.
     */
    public RedisAsyncCommands<String, String> getAsyncCommands() {
        if (connection == null || !connection.isOpen()) {
            connectionLock.lock();
            try {
                if (connection == null || !connection.isOpen()) {
                    logger.warn("Redis connection lost. Attempting to reconnect...");
                    reconnectInternal();
                }
            } finally {
                connectionLock.unlock();
            }
        }
        return connection.async();
    }

    /**
     * Internal reconnect logic, must be called with connectionLock held.
     */
    private void reconnectInternal() {
        if (connection != null) {
            try { connection.close(); } catch (Exception ignored) {}
        }
        connection = createConnectionWithRetry();
    }

    /**
     * Reconnects to Redis, closing the old connection if needed.
     * Prevents multiple threads from reconnecting simultaneously.
     */
    public void reconnect() {
        connectionLock.lock();
        try {
            reconnectInternal();
        } finally {
            connectionLock.unlock();
        }
    }

    private StatefulRedisConnection<String, String> createConnectionWithRetry() {
        int attempts = 0;
        while (attempts < 3) {
            try {
                return redisClient.connect();
            } catch (Exception e) {
                attempts++;
                logger.error("Redis connection attempt {} failed", attempts, e);
                try { Thread.sleep(connectionTimeout.toMillis()); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw new IllegalStateException("Unable to connect to Redis after retries");
    }

    /**
     * Managed lifecycle: Connection is managed and closed only at application shutdown.
     * Do not call this method directly; it is invoked by the container (e.g., Spring) via @PreDestroy.
     */
    @PreDestroy
    public void shutdown() {
        connectionLock.lock();
        try {
            if (connection != null) {
                try { connection.close(); } catch (Exception ignored) {}
                connection = null;
            }
            if (redisClient != null) {
                try { redisClient.shutdown(); } catch (Exception ignored) {}
            }
        } finally {
            connectionLock.unlock();
        }
    }

    /**
     * Thread safety: This provider is intended to be a singleton bean managed by Spring.
     * Lettuce connections and commands are thread-safe for sync/async usage.
     */
}
