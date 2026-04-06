package com.example.ratelimiter.spring.resolver;

import com.example.ratelimiter.spring.model.KeyType;
import com.example.ratelimiter.spring.model.RateLimiterRule;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class KeyResolverFactory {
    private final Map<KeyType, KeyResolver> resolverMap = new EnumMap<>(KeyType.class);
    private final IpKeyResolver ipKeyResolver;
    private final HeaderKeyResolver headerKeyResolver;
    private final CustomKeyResolver customKeyResolver;

    @Autowired
    public KeyResolverFactory(IpKeyResolver ipKeyResolver, HeaderKeyResolver headerKeyResolver, CustomKeyResolver customKeyResolver) {
        this.ipKeyResolver = ipKeyResolver;
        this.headerKeyResolver = headerKeyResolver;
        this.customKeyResolver = customKeyResolver;
        resolverMap.put(KeyType.IP, ipKeyResolver);
        resolverMap.put(KeyType.HEADER, headerKeyResolver);
        resolverMap.put(KeyType.CUSTOM, customKeyResolver);
        // USER_ID can be added later
    }

    public KeyResolver getResolver(KeyType keyType) {
        return resolverMap.get(keyType);
    }

    public String resolveKey(KeyType keyType, HttpServletRequest request, RateLimiterRule rule) {
        KeyResolver resolver = getResolver(keyType);
        return resolver != null ? resolver.resolveKey(request, rule) : null;
    }
}
