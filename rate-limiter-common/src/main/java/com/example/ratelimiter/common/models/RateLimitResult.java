package com.example.ratelimiter.common.models;

/**
 * Immutable result of a rate limit check.
 */
public final class RateLimitResult {
    private final boolean allowed;
    private final long remaining;
    private final long retryAfter;
    private final long capacity;

    public RateLimitResult(boolean allowed, long remaining, long retryAfter, long capacity) {
        this.allowed = allowed;
        this.remaining = remaining;
        this.retryAfter = retryAfter;
        this.capacity = capacity;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getRemaining() {
        return remaining;
    }

    public long getRetryAfter() {
        return retryAfter;
    }

    public long getCapacity() {
        return capacity;
    }

    public static RateLimitResult fallback() {
        return new RateLimitResult(false, 0, 1, 0);
    }

    @Override
    public String toString() {
        return "RateLimitResult{" +
                "allowed=" + allowed +
                ", remaining=" + remaining +
                ", retryAfter=" + retryAfter +
                ", capacity=" + capacity +
                '}';
    }
}
