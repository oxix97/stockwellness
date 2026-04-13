package org.stockwellness.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;

/**
 * API 모듈 전용 Redis serializer 팩토리.
 * Security 타입 캐시는 Spring Security 모듈이 포함된 serializer를 사용한다.
 */
public final class ApiRedisSerializerConfig {

    private ApiRedisSerializerConfig() {}

    public static GenericJackson2JsonRedisSerializer createDomainSerializer() {
        return RedisSerializerConfig.createDomainSerializer();
    }

    public static GenericJackson2JsonRedisSerializer createSecuritySerializer(ClassLoader classLoader) {
        ObjectMapper mapper = RedisSerializerConfig.createBaseObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(classLoader));
        mapper.setDefaultTyping(RedisSerializerConfig.createTypeResolver(mapper));
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
