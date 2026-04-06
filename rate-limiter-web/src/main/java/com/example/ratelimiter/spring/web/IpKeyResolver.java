package com.example.ratelimiter.spring.web;

import com.example.ratelimiter.spring.model.RateLimiterRule;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class IpKeyResolver implements KeyResolver {
    @Override
    public String resolveKey(HttpServletRequest request, RateLimiterRule rule) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        } else {
            // X-Forwarded-For can contain multiple IPs, take the first
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
