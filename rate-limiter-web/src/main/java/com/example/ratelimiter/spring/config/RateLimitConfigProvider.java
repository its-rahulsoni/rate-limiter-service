package com.example.ratelimiter.spring.config;

import com.example.ratelimiter.spring.model.KeyType;
import com.example.ratelimiter.spring.model.RateLimiterRule;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RateLimitConfigProvider {
    private final RateLimiterProperties properties;
    private final Validator validator;

    @Autowired
    public RateLimitConfigProvider(RateLimiterProperties properties, Validator validator) {
        this.properties = properties;
        this.validator = validator;
        validateRules();
    }

    private void validateRules() {
        if (properties.getRules() != null) {
            for (RateLimiterRule rule : properties.getRules()) {
                Set<ConstraintViolation<RateLimiterRule>> violations = validator.validate(rule);
                if (rule.getKeyType() == com.example.ratelimiter.spring.model.KeyType.HEADER && (rule.getHeaderName() == null || rule.getHeaderName().isEmpty())) {
                    throw new IllegalArgumentException("headerName must be present for HEADER type");
                }
                if (!violations.isEmpty()) {
                    StringBuilder sb = new StringBuilder("Invalid RateLimiterRule configuration:\n");
                    for (ConstraintViolation<RateLimiterRule> v : violations) {
                        sb.append(" - ").append(v.getPropertyPath()).append(": ").append(v.getMessage()).append("\n");
                    }
                    throw new IllegalArgumentException(sb.toString());
                }
            }
        }
    }

    /**
     * Returns all rules loaded from configuration.
     */
    public List<RateLimiterRule> getAllRules() {
        List<RateLimiterRule> rules = properties.getRules();
        return rules != null ? rules : Collections.emptyList();
    }

    /**
     * Returns rules applicable for a given KeyType.
     */
    public List<RateLimiterRule> getRulesForKeyType(KeyType keyType) {
        return getAllRules().stream()
                .filter(rule -> Objects.equals(rule.getKeyType(), keyType))
                .collect(Collectors.toList());
    }

    /**
     * Returns the first matching rule for a given KeyType and optional headerName.
     * Returns null if not found.
     */
    public RateLimiterRule getRule(KeyType keyType, String headerName) {
        return getAllRules().stream()
                .filter(rule -> Objects.equals(rule.getKeyType(), keyType))
                .filter(rule -> keyType != KeyType.HEADER || Objects.equals(rule.getHeaderName(), headerName))
                .findFirst()
                .orElse(null);
    }
}
