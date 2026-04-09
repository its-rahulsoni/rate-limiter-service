package com.example.ratelimiter.spring.config;

import com.example.ratelimiter.common.enums.KeyType;
import com.example.ratelimiter.common.models.RateLimiterRule;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitConfigProviderTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void loadsValidRules() {
        RateLimiterRule rule = new RateLimiterRule();
        rule.setKeyType(KeyType.IP);
        rule.setLimit(10);
        rule.setWindow(60);
        RateLimiterProperties props = new RateLimiterProperties();
        // Updated for new config: rules is now a Map<String, RateLimiterRule>
        props.setRules(Collections.singletonMap("testRule", rule));
        RateLimitConfigProvider provider = new RateLimitConfigProvider(props, validator);
        assertEquals(1, provider.getRule("testRule") != null ? 1 : 0);
    }

    @Test
    void failsOnInvalidLimit() {
        RateLimiterRule rule = new RateLimiterRule();
        rule.setKeyType(KeyType.IP);
        rule.setLimit(0);
        rule.setWindow(60);
        RateLimiterProperties props = new RateLimiterProperties();
        props.setRules(Collections.singletonMap("testRule", rule));
        assertThrows(IllegalArgumentException.class, () -> new RateLimitConfigProvider(props, validator));
    }

    @Test
    void failsOnMissingKeyType() {
        RateLimiterRule rule = new RateLimiterRule();
        rule.setKeyType(null);
        rule.setLimit(10);
        rule.setWindow(60);
        RateLimiterProperties props = new RateLimiterProperties();
        props.setRules(Collections.singletonMap("testRule", rule));
        assertThrows(IllegalArgumentException.class, () -> new RateLimitConfigProvider(props, validator));
    }

    @Test
    void failsOnHeaderTypeWithoutHeaderName() {
        RateLimiterRule rule = new RateLimiterRule();
        rule.setKeyType(KeyType.HEADER);
        rule.setLimit(10);
        rule.setWindow(60);
        rule.setHeaderName(null);
        RateLimiterProperties props = new RateLimiterProperties();
        props.setRules(Collections.singletonMap("testRule", rule));
        assertThrows(IllegalArgumentException.class, () -> new RateLimitConfigProvider(props, validator));
    }
}
