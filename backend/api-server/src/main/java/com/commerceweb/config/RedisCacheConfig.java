package com.commerceweb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;

@Configuration
@EnableCaching
public class RedisCacheConfig {
    // Spring Boot가 자동으로 Redis 캐시 설정을 처리합니다.
}
