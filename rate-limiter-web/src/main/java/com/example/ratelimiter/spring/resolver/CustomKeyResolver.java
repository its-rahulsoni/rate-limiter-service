package com.example.ratelimiter.spring.resolver;

import com.example.ratelimiter.spring.model.RateLimiterRule;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Flexible resolver for CUSTOM key type. Supports extracting from query param, header, or JWT (future extension).
 * Extraction strategy is defined by config fields in RateLimiterRule: customParam, customHeader, customJwtClaim.
 */
@Component
public class CustomKeyResolver implements KeyResolver {
    @Override
    public String resolveKey(HttpServletRequest request, RateLimiterRule rule) {
        // Example: Try query param first, then header, then (optionally) JWT claim
        if (rule == null) return null;
        // Query param
        if (rule.getCustomParam() != null) {
            String value = request.getParameter(rule.getCustomParam());
            if (value != null && !value.isEmpty()) return value;
        }
        // Header
        if (rule.getCustomHeader() != null) {
            String value = request.getHeader(rule.getCustomHeader());
            if (value != null && !value.isEmpty()) return value;
        }
        // JWT claim (future extension, not implemented here)
        // if (rule.getCustomJwtClaim() != null) { ... }
        return null;
    }
}
