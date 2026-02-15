package org.stockwellness.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;

/**
 * [Redis Serializer Factory]
 * Redis 직렬화/역직렬화에 필요한 ObjectMapper 설정을 전담합니다.
 * Java 21 Record, Security, 날짜 포맷 등을 처리합니다.
 */
public final class RedisSerializerConfig {

    private RedisSerializerConfig() {}

    public static GenericJackson2JsonRedisSerializer createDomainSerializer() {
        ObjectMapper mapper = createBaseObjectMapper();
        mapper.setDefaultTyping(createTypeResolver(mapper));
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    public static GenericJackson2JsonRedisSerializer createSecuritySerializer(ClassLoader classLoader) {
        ObjectMapper mapper = createBaseObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(classLoader));
        mapper.setDefaultTyping(createTypeResolver(mapper));
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    private static ObjectMapper createBaseObjectMapper() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("org.stockwellness")
                .allowIfBaseType("java.util")
                .allowIfBaseType("java.time")
                .allowIfBaseType("org.springframework.security")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setPolymorphicTypeValidator(ptv);

        return mapper;
    }

    private static TypeResolverBuilder<?> createTypeResolver(ObjectMapper mapper) {
        return new ObjectMapper.DefaultTypeResolverBuilder(
                ObjectMapper.DefaultTyping.NON_FINAL, 
                mapper.getPolymorphicTypeValidator()
        ) {
            @Override
            public boolean useForType(JavaType t) {
                if (t.isPrimitive()) return false;
                if (t.isTypeOrSubTypeOf(String.class)) return false;
                if (t.isTypeOrSubTypeOf(Number.class)) return false;
                if (t.isTypeOrSubTypeOf(Boolean.class)) return false;
                if (t.isArrayType()) return false;
                
                return true;
            }
        }.init(JsonTypeInfo.Id.CLASS, null)
         .inclusion(JsonTypeInfo.As.PROPERTY)
         .typeProperty("@class");
    }
}
