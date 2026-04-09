package com.example.ratelimiter.spring.web;

import com.example.ratelimiter.common.models.RateLimiterRule;
import com.example.ratelimiter.spring.resolver.CustomKeyResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomKeyResolverTest {
    private final CustomKeyResolver resolver = new CustomKeyResolver();
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final RateLimiterRule rule = new RateLimiterRule();

    @Test
    void resolvesFromQueryParam() {
        rule.setCustomParam("userId");
        when(request.getParameter("userId")).thenReturn("abc");
        assertEquals("abc", resolver.resolveKey(request, rule));
    }

    @Test
    void resolvesFromHeaderIfNoQueryParam() {
        rule.setCustomParam("userId");
        rule.setCustomHeader("X-User-Id");
        when(request.getParameter("userId")).thenReturn("");
        when(request.getHeader("X-User-Id")).thenReturn("headerVal");
        assertEquals("headerVal", resolver.resolveKey(request, rule));
    }

    @Test
    void returnsNullIfNoCustomFields() {
        assertNull(resolver.resolveKey(request, rule));
    }
}
