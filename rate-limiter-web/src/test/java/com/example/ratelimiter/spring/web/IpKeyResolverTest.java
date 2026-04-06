package com.example.ratelimiter.spring.web;

import com.example.ratelimiter.spring.model.RateLimiterRule;
import com.example.ratelimiter.spring.resolver.IpKeyResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class IpKeyResolverTest {
    private final IpKeyResolver resolver = new IpKeyResolver();
    private final RateLimiterRule dummyRule = new RateLimiterRule();

    @Test
    void resolvesIpFromXForwardedFor() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");
        String key = resolver.resolveKey(req, dummyRule);
        assertEquals("1.2.3.4", key);
    }

    @Test
    void resolvesIpFromRemoteAddrIfNoHeader() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        Mockito.when(req.getRemoteAddr()).thenReturn("9.8.7.6");
        String key = resolver.resolveKey(req, dummyRule);
        assertEquals("9.8.7.6", key);
    }

    @Test
    void returnsNullIfNoIp() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        Mockito.when(req.getRemoteAddr()).thenReturn(null);
        String key = resolver.resolveKey(req, dummyRule);
        assertNull(key);
    }
}
