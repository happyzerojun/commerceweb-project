package com.commerceweb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;

@Configuration
@EnableCaching  // Spring Boot가 캐시 인프라를 자동으로 구성
public class RedisCacheConfig {
    // Spring Boot가 자동으로 Redis 캐시 설정을 처리합니다.

    // Redis 캐시는 데이터를 빠르게 읽고 처리하기 위해 메모리에 미리 저장해두는 임시 저장소

//    캐시 조회: 애플리케이션이 필요한 데이터를 먼저 캐시에서 찾습니다.
//
//    캐시 히트: 데이터가 캐시에 있으면 즉시 반환합니다.
//
//    캐시 미스: 데이터가 없으면 실제 데이터베이스에서 조회합니다.
//
//    캐시 저장: 데이터베이스에서 가져온 데이터를 캐시에 저장합니다(TTL 설정 가능).

//    캐싱 비즈니스 로직에만 집중할 수 있고, 복잡한 Spring Bean 등록이나 설정 코드는 작성할 필요가 없습니다.
//    이것이 Spring Boot의 "자동 설정(Auto Configuration)"의 강력함
}
