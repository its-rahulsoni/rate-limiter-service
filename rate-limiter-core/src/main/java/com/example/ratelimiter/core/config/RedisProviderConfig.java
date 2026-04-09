package com.example.ratelimiter.core.config;

import com.example.ratelimiter.core.redis.RedisClientProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisProviderConfig {
    @Bean
    public RedisClientProvider redisClientProvider(RedisProperties redisProperties) {
        return new RedisClientProvider(
            redisProperties.getUrl(),
            redisProperties.getTimeout(),
            redisProperties.getCommandTimeout()
        );
    }
}
