package com.example.ratelimiter.spring.web;

import com.example.ratelimiter.common.enums.KeyType;
import com.example.ratelimiter.common.models.RateLimiterRule;
import com.example.ratelimiter.spring.config.RateLimitConfigProvider;
import com.example.ratelimiter.spring.limiter.RateLimiter;
import com.example.ratelimiter.spring.resolver.KeyResolverFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitingInterceptorTest {
    private RateLimitConfigProvider configProvider;
    private KeyResolverFactory keyResolverFactory;
    private RateLimiter rateLimiter;
    private RateLimitingInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private PrintWriter writer;

    @BeforeEach
    void setup() throws Exception {
        configProvider = mock(RateLimitConfigProvider.class);
        keyResolverFactory = mock(KeyResolverFactory.class);
        rateLimiter = mock(RateLimiter.class);
        interceptor = new RateLimitingInterceptor(configProvider, keyResolverFactory, rateLimiter);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    void allowsRequestWhenAllRulesPass() throws Exception {
        RateLimiterRule rule = new RateLimiterRule();
        rule.setKeyType(KeyType.IP);
        when(configProvider.getGlobalRules()).thenReturn(List.of(rule));
        when(keyResolverFactory.resolveKey(eq(KeyType.IP), any(), eq(rule))).thenReturn("1.2.3.4");
        when(rateLimiter.allowRequest("1.2.3.4", rule)).thenReturn(true);
        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
        verify(response, never()).setStatus(429);
    }

    @Test
    void rejectsRequestWhenAnyRuleFails() throws Exception {
        RateLimiterRule rule = new RateLimiterRule();
        rule.setKeyType(KeyType.IP);
        when(configProvider.getGlobalRules()).thenReturn(List.of(rule));
        when(keyResolverFactory.resolveKey(eq(KeyType.IP), any(), eq(rule))).thenReturn("1.2.3.4");
        when(rateLimiter.allowRequest("1.2.3.4", rule)).thenReturn(false);
        boolean result = interceptor.preHandle(request, response, new Object());
        assertFalse(result);
        verify(response).setStatus(429);
        verify(writer).write(contains("Rate limit exceeded"));
    }

    @Test
    void skipsRuleWhenKeyIsNull() throws Exception {
        RateLimiterRule rule = new RateLimiterRule();
        rule.setKeyType(KeyType.IP);
        when(configProvider.getGlobalRules()).thenReturn(List.of(rule));
        when(keyResolverFactory.resolveKey(eq(KeyType.IP), any(), eq(rule))).thenReturn(null);
        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
        verify(response, never()).setStatus(429);
    }

    @Test
    void skipsRuleWhenKeyIsEmpty() throws Exception {
        RateLimiterRule rule = new RateLimiterRule();
        rule.setKeyType(KeyType.IP);
        when(configProvider.getGlobalRules()).thenReturn(List.of(rule));
        when(keyResolverFactory.resolveKey(eq(KeyType.IP), any(), eq(rule))).thenReturn("");
        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
        verify(response, never()).setStatus(429);
    }
}
