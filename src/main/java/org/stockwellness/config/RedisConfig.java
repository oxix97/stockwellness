package org.stockwellness.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // 보안을 위한 화이트리스트 기반 TypeValidator 설정
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("org.stockwellness") // 우리 프로젝트 클래스 허용
                .allowIfBaseType("java.util")         // List, Map 등 허용
                .allowIfBaseType("java.time")         // 날짜/시간 타입 허용
                .allowIfBaseType("java.lang")         // Long, String 등 허용
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // A. 기본 설정 (Default): TTL 1시간, JSON 직렬화
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // 기본 1시간
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer)); // 수정된 Serializer 사용

        // B. 캐시 이름별 개별 설정 (Custom Configurations)
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();

        // (1) AI 분석 결과: 하루 종일 유지 (다음날 장 시작 전까지)
        configMap.put("ai_analysis", defaultConfig.entryTtl(Duration.ofHours(24)));

        // (2) 주식 기본 정보: 변동이 적으니 7일 유지
        configMap.put("stock_info", defaultConfig.entryTtl(Duration.ofDays(7)));

        // (3) 짧은 캐시: 10분
        configMap.put("short_lived", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // (4) 회원 정보 캐시: 10분 (CustomUserDetailsService에서 사용)
        configMap.put("member", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configMap) // 맵 적용
                .build();
    }
}