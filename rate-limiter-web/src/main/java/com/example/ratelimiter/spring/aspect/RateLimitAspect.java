package com.example.ratelimiter.spring.aspect;

import com.example.ratelimiter.common.models.RateLimiterRule;
import com.example.ratelimiter.spring.annotation.RateLimit;
import com.example.ratelimiter.spring.config.RateLimitConfigProvider;
import com.example.ratelimiter.spring.limiter.RateLimiter;
import com.example.ratelimiter.spring.resolver.KeyResolverFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class RateLimitAspect {
    private final KeyResolverFactory keyResolverFactory;
    private final RateLimiter rateLimiter;
    private final RateLimitConfigProvider configProvider;

    public RateLimitAspect(KeyResolverFactory keyResolverFactory, RateLimiter rateLimiter, RateLimitConfigProvider configProvider) {
        this.keyResolverFactory = keyResolverFactory;
        this.rateLimiter = rateLimiter;
        this.configProvider = configProvider;
    }

    @Around("@annotation(com.example.ratelimiter.spring.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

        // Respect mode: skip if INTERCEPTOR only
        String mode = configProvider.getMode();
        if ("INTERCEPTOR".equalsIgnoreCase(mode)) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        // Extract rule name from annotation
        String ruleName = rateLimit.rule();
        if (ruleName == null || ruleName.isEmpty()) {
            throw new IllegalArgumentException("@RateLimit rule name must be specified");
        }

        // Fetch RateLimiterRule from config provider
        RateLimiterRule rule = configProvider.getRule(ruleName);
        if (rule == null) {
            throw new IllegalStateException("No RateLimiterRule found for rule name: " + ruleName);
        }

        // Extract HttpServletRequest
        HttpServletRequest request = getCurrentHttpRequest();
        if (request == null) {
            throw new IllegalStateException("No HttpServletRequest available for rate limiting");
        }

        // Resolve key
        String key = keyResolverFactory.resolveKey(rule.getKeyType(), request, rule);
        if (key == null || key.isEmpty()) {
            // Optionally: allow or block if key cannot be resolved
            return joinPoint.proceed();
        }

        // Call RateLimiter
        boolean allowed = rateLimiter.allowRequest(key, rule);
        if (!allowed) {
            throw new RateLimitExceededException("Rate limit exceeded");
        }
        return joinPoint.proceed();
    }

    private HttpServletRequest getCurrentHttpRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
