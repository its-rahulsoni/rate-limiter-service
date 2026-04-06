package com.example.ratelimiter.spring.web;

import com.example.ratelimiter.spring.model.RateLimiterRule;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class HeaderKeyResolverTest {
    private final HeaderKeyResolver resolver = new HeaderKeyResolver();

    @Test
    void resolvesHeaderValue() {
        RateLimiterRule rule = new RateLimiterRule();
        rule.setHeaderName("X-API-KEY");
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader("X-API-KEY")).thenReturn("abc123");
        String key = resolver.resolveKey(req, rule);
        assertEquals("abc123", key);
    }

    @Test
    void returnsNullIfHeaderNameMissing() {
        RateLimiterRule rule = new RateLimiterRule();
        rule.setHeaderName(null);
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        String key = resolver.resolveKey(req, rule);
        assertNull(key);
    }

    @Test
    void returnsNullIfHeaderNotPresent() {
        RateLimiterRule rule = new RateLimiterRule();
        rule.setHeaderName("X-API-KEY");
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader("X-API-KEY")).thenReturn(null);
        String key = resolver.resolveKey(req, rule);
        assertNull(key);
    }
}
