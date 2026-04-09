package com.example.ratelimiter.spring.config;


import com.example.ratelimiter.common.enums.KeyType;
import com.example.ratelimiter.common.models.RateLimiterRule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RateLimitConfigProvider {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitConfigProvider.class);
    private final RateLimiterProperties properties;
    private final Validator validator;

    @Autowired
    public RateLimitConfigProvider(RateLimiterProperties properties, Validator validator) {
        this.properties = properties;
        this.validator = validator;
        validateAllRules();
    }

    private void validateAllRules() {
        // Validate rules map
        if (properties.getRules() != null) {
            for (Map.Entry<String, RateLimiterRule> entry : properties.getRules().entrySet()) {
                String ruleName = entry.getKey();
                RateLimiterRule rule = entry.getValue();
                validateSingleRule(rule, "rules[" + ruleName + "]");
            }
        }
        // Validate global rules list
        if (properties.getGlobalRules() != null) {
            int idx = 0;
            for (RateLimiterRule rule : properties.getGlobalRules()) {
                validateSingleRule(rule, "globalRules[" + idx + "]");
                idx++;
            }
        }
    }

    private void validateSingleRule(RateLimiterRule rule, String context) {
        if (rule == null) {
            throw new IllegalArgumentException("Null RateLimiterRule in " + context);
        }
        Set<ConstraintViolation<RateLimiterRule>> violations = validator.validate(rule);
        if (rule.getKeyType() == KeyType.HEADER && (rule.getHeaderName() == null || rule.getHeaderName().isEmpty())) {
            throw new IllegalArgumentException("headerName must be present for HEADER type in " + context);
        }
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder("Invalid RateLimiterRule configuration in " + context + ":\n");
            for (ConstraintViolation<RateLimiterRule> v : violations) {
                sb.append(" - ").append(v.getPropertyPath()).append(": ").append(v.getMessage()).append("\n");
            }
            throw new IllegalArgumentException(sb.toString());
        }
    }

    /**
     * Returns the first matching rule for a given rule name.
     * Returns null if not found.
     */
    public RateLimiterRule getRule(String ruleName) {
        Map<String, RateLimiterRule> rules = properties.getRules();
        if (rules == null) {
            logger.warn("No rules map configured in rate-limiter config");
            return null;
        }
        RateLimiterRule rule = rules.get(ruleName);
        if (rule == null) {
            logger.warn("No RateLimiterRule found for rule name: {}", ruleName);
        }
        return rule;
    }

    public String getMode() {
        return properties.getMode();
    }

    public List<RateLimiterRule> getGlobalRules() {
        List<RateLimiterRule> rules = properties.getGlobalRules();
        return rules != null ? rules : Collections.emptyList();
    }
}
