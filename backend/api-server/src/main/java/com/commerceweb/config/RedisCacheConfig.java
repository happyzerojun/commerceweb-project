package com.commerceweb.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Redis 캐시 설정 (TS-08).
 * <ul>
 *   <li>TTL: application.properties의 spring.cache.redis.time-to-live (1시간). 무효화가 누락되더라도 오래된 추천이 영원히 남지 않게 하는 안전망.</li>
 *   <li>transactionAware: @CacheEvict/@CachePut을 트랜잭션 커밋 후에 실행한다.
 *       커밋 전에 지우면, 그 사이 다른 요청이 아직 커밋 안 된(=옛) 데이터로 추천을 다시 계산해 캐시에 넣을 수 있고,
 *       롤백돼도 캐시만 지워지는 불일치가 생긴다.</li>
 *   <li>SCAN 배치 삭제: allEntries 무효화는 키 패턴 삭제다. 기본 KEYS 명령은 키가 많으면 Redis를 블로킹하므로 SCAN(1000개씩)으로 지운다.</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(RedisConnectionFactory connectionFactory) {
        return builder -> builder
                .cacheWriter(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory, BatchStrategies.scan(1000)))
                .transactionAware();
    }
}
