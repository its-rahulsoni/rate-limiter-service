package com.example.ratelimiter.core.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan(basePackageClasses = RedisProperties.class)
public class RedisConfig {
    // This ensures RedisProperties is registered as a Spring bean for the core module.
}
