package com.example.ratelimiter.spring.web;

import com.example.ratelimiter.spring.model.RateLimiterRule;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class HeaderKeyResolver implements KeyResolver {
    @Override
    public String resolveKey(HttpServletRequest request, RateLimiterRule rule) {
        if (rule == null || rule.getHeaderName() == null) {
            return null;
        }
        return request.getHeader(rule.getHeaderName());
    }
}
