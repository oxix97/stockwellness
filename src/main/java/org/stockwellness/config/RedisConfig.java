package org.stockwellness.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key: String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value: JSON (Domain Serializer 사용)
        // 복잡한 설정은 RedisSerializerConfig에 위임하여 코드가 깔끔해짐
        GenericJackson2JsonRedisSerializer jsonSerializer = RedisSerializerConfig.createDomainSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Primary
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // 1. Serializer 가져오기 (Factory 메서드 호출)
        var domainSerializer = RedisSerializerConfig.createDomainSerializer();
        var securitySerializer = RedisSerializerConfig.createSecuritySerializer(getClass().getClassLoader());

        // 2. 캐시 설정 구성
        RedisCacheConfiguration defaultConfig = createCacheConfig(domainSerializer, Duration.ofHours(1));

        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        // AI 분석 (Long Term)
        configMap.put("ai_analysis", createCacheConfig(domainSerializer, Duration.ofHours(24)));
        // 주식 정보 (Mid Term)
        configMap.put("stock_info", createCacheConfig(domainSerializer, Duration.ofDays(7)));
        // 인증 정보 (Short Term & Security Serializer)
        configMap.put("member", createCacheConfig(securitySerializer, Duration.ofMinutes(30)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configMap)
                .build();
    }

    // 캐시 설정을 만드는 헬퍼 메서드 (반복 코드 제거)
    private RedisCacheConfiguration createCacheConfig(GenericJackson2JsonRedisSerializer serializer, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}