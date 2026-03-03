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
import org.stockwellness.global.config.CacheType;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class ApiRedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        GenericJackson2JsonRedisSerializer jsonSerializer = RedisSerializerConfig.createDomainSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Primary
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        var domainSerializer = RedisSerializerConfig.createDomainSerializer();
        var securitySerializer = RedisSerializerConfig.createSecuritySerializer(getClass().getClassLoader());

        // 기본 설정 (1시간 TTL)
        RedisCacheConfiguration defaultConfig = createCacheConfig(domainSerializer, Duration.ofHours(1));

        // Enum 기반 자동 설정 맵 생성
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        for (CacheType type : CacheType.values()) {
            var serializer = type.isUseSecuritySerializer() ? securitySerializer : domainSerializer;
            configMap.put(type.getCacheName(), createCacheConfig(serializer, type.getTtl()));
        }

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configMap)
                .build();
    }

    private RedisCacheConfiguration createCacheConfig(GenericJackson2JsonRedisSerializer serializer, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}
