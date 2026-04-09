package com.example.ratelimiter.core.util;

/**
 * Utility for building safe Redis keys for rate limiting.
 */
public class KeyBuilder {
    /**
     * Builds a Redis key for a given rule and key.
     * Format: rate_limit:{rule}:{key}
     * @param rule the rule name or algorithm identifier
     * @param key the unique identifier (IP, user, etc.)
     * @return formatted Redis key
     */
    public String build(String rule, String key) {
        return "rate_limit:" + sanitize(rule) + ":" + sanitize(key);
    }

    private String sanitize(String input) {
        if (input == null) return "null";
        // Replace unsafe characters with underscore
        return input.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
