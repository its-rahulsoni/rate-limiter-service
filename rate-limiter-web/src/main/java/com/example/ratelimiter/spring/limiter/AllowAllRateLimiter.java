package com.example.ratelimiter.spring.limiter;

import com.example.ratelimiter.common.models.RateLimiterRule;
import org.springframework.stereotype.Component;

@Component
public class AllowAllRateLimiter implements RateLimiter {
    @Override
    public boolean allowRequest(String key, RateLimiterRule rule) {
        // Stub: always allow
        return true;
    }
}
