package com.example.ratelimiter.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan({"com.example.ratelimiter.core.config"})
public class RateLimiterApplication {
    public static void main(String[] args) {
        SpringApplication.run(RateLimiterApplication.class, args);
    }
}
