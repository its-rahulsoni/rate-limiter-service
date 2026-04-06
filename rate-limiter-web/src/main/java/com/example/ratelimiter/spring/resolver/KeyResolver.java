package com.example.ratelimiter.spring.resolver;

import com.example.ratelimiter.spring.model.RateLimiterRule;
import jakarta.servlet.http.HttpServletRequest;

public interface KeyResolver {
    /**
     * Resolve a unique key from the request for rate limiting.
     * @param request HttpServletRequest
     * @param rule RateLimiterRule
     * @return resolved key or null if not resolvable
     */
    String resolveKey(HttpServletRequest request, RateLimiterRule rule);
}
