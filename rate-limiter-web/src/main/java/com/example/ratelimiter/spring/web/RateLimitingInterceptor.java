package com.example.ratelimiter.spring.web;

import com.example.ratelimiter.spring.config.RateLimitConfigProvider;
import com.example.ratelimiter.spring.limiter.RateLimiter;
import com.example.ratelimiter.spring.model.KeyType;
import com.example.ratelimiter.spring.model.RateLimiterRule;
import com.example.ratelimiter.spring.resolver.KeyResolverFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingInterceptor.class);
    private final RateLimitConfigProvider configProvider;
    private final KeyResolverFactory keyResolverFactory;
    private final RateLimiter rateLimiter;

    public RateLimitingInterceptor(RateLimitConfigProvider configProvider,
                                   KeyResolverFactory keyResolverFactory,
                                   RateLimiter rateLimiter) {
        this.configProvider = configProvider;
        this.keyResolverFactory = keyResolverFactory;
        this.rateLimiter = rateLimiter;
    }

    /**
     * All the rules are applied to each request. If any rule is violated, the request is rejected with a 429 status code.
     * If the request does not have property for a rule, that rule is simply skipped. This allows for flexible configuration where different rules can apply to
     * different sets of requests based on the presence of certain properties (e.g., headers, IP address, etc.).
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        List<RateLimiterRule> rules = configProvider.getAllRules();
        for (RateLimiterRule rule : rules) {
            KeyType keyType = rule.getKeyType();
            String key = keyResolverFactory.resolveKey(keyType, request, rule);
            if (key == null || key.isEmpty()) {
                String reason = (key == null) ? "key not found (null)" : "key is empty";
                if (keyType == KeyType.HEADER) {
                    logger.info("Skipping rule {} (header: {}) — {}", keyType, rule.getHeaderName(), reason);
                } else {
                    logger.info("Skipping rule {} — {}", keyType, reason);
                }
                continue;
            }
            boolean allowed = rateLimiter.allowRequest(key, rule);
            if (!allowed) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Rate limit exceeded\"}");
                return false;
            }
        }
        return true;
    }
}
