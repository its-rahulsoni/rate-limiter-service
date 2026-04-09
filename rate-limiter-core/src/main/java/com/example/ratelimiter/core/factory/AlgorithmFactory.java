package com.example.ratelimiter.core.factory;

import com.example.ratelimiter.common.enums.AlgorithmType;
import com.example.ratelimiter.core.algorithm.RateLimitingAlgorithm;
import java.util.EnumMap;
import java.util.Map;

/**
 * Factory for mapping algorithm type to RateLimitingAlgorithm implementation.
 * Supports easy extensibility for new algorithms.
 */
public class AlgorithmFactory {
    private final Map<AlgorithmType, RateLimitingAlgorithm> algorithmMap = new EnumMap<>(AlgorithmType.class);

    public AlgorithmFactory(Map<AlgorithmType, RateLimitingAlgorithm> algorithms) {
        if (algorithms != null) {
            algorithmMap.putAll(algorithms);
        }
    }

    /**
     * Returns the correct RateLimitingAlgorithm for the given type.
     * @param type AlgorithmType (e.g., TOKEN_BUCKET)
     * @return RateLimitingAlgorithm implementation
     * @throws IllegalArgumentException if no implementation is registered
     */
    public RateLimitingAlgorithm getAlgorithm(AlgorithmType type) {
        RateLimitingAlgorithm algo = algorithmMap.get(type);
        if (algo == null) {
            throw new IllegalArgumentException("No algorithm registered for type: " + type);
        }
        return algo;
    }

    /**
     * Register or override an algorithm implementation.
     */
    public void register(AlgorithmType type, RateLimitingAlgorithm algorithm) {
        algorithmMap.put(type, algorithm);
    }
}
