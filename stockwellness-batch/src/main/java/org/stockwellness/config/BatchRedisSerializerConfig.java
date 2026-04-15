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

/**
 * Batch 모듈 전용 Redis serializer 팩토리.
 */
public final class BatchRedisSerializerConfig {

    private BatchRedisSerializerConfig() {}

    public static GenericJackson2JsonRedisSerializer createDomainSerializer() {
        ObjectMapper mapper = createBaseObjectMapper();
        mapper.setDefaultTyping(createTypeResolver(mapper));
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    static ObjectMapper createBaseObjectMapper() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("org.stockwellness")
                .allowIfBaseType("java.util")
                .allowIfBaseType("java.time")
                .allowIfSubType("org.stockwellness")
                .allowIfSubType("java.util")
                .allowIfSubType("java.time")
                .allowIfSubType("java.lang")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setPolymorphicTypeValidator(ptv);

        return mapper;
    }

    static TypeResolverBuilder<?> createTypeResolver(ObjectMapper mapper) {
        return new ObjectMapper.DefaultTypeResolverBuilder(
                ObjectMapper.DefaultTyping.NON_FINAL,
                mapper.getPolymorphicTypeValidator()
        ) {
            @Override
            public boolean useForType(JavaType type) {
                if (type.isPrimitive()) return false;
                if (type.isTypeOrSubTypeOf(String.class)) return false;
                if (type.isTypeOrSubTypeOf(Number.class)) return false;
                if (type.isTypeOrSubTypeOf(Boolean.class)) return false;
                if (type.isArrayType()) return false;

                return true;
            }
        }.init(JsonTypeInfo.Id.CLASS, null)
                .inclusion(JsonTypeInfo.As.PROPERTY)
                .typeProperty("@class");
    }
}
