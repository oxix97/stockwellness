package org.stockwellness.config;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.MemberRole;
import org.stockwellness.global.security.MemberPrincipal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisSerializerCompatibilityTest {

    private final GenericJackson2JsonRedisSerializer apiDomainSerializer =
            ApiRedisSerializerConfig.createDomainSerializer();

    private final GenericJackson2JsonRedisSerializer apiSecuritySerializer =
            ApiRedisSerializerConfig.createSecuritySerializer(getClass().getClassLoader());

    private final GenericJackson2JsonRedisSerializer coreFallbackSerializer =
            ApiRedisSerializerConfig.createDomainSerializer();

    @Test
    void domainSerializer_includesClassMetadataInPayload() {
        SampleCacheValue source = new SampleCacheValue(
                "alpha",
                LocalDateTime.of(2026, 4, 13, 10, 30),
                List.of("KOSPI", "AI")
        );

        byte[] serialized = apiDomainSerializer.serialize(source);
        String payload = new String(serialized, StandardCharsets.UTF_8);

        assertThat(payload).contains("\"@class\"");
        assertThat(payload).contains(SampleCacheValue.class.getName());
    }

    @Test
    void coreFallbackSerializer_canReadSimplePayloadWithoutJavaTime() {
        SimpleCacheValue source = new SimpleCacheValue("alpha", List.of("KOSPI", "AI"));

        byte[] serialized = apiDomainSerializer.serialize(source);
        Object deserialized = coreFallbackSerializer.deserialize(serialized);

        assertThat(deserialized).isEqualTo(source);
    }

    @Test
    void coreFallbackSerializer_canReadDomainPayloadContainingJavaTime() {
        SampleCacheValue source = new SampleCacheValue(
                "alpha",
                LocalDateTime.of(2026, 4, 13, 10, 30),
                List.of("KOSPI", "AI")
        );

        byte[] serialized = apiDomainSerializer.serialize(source);
        Object deserialized = coreFallbackSerializer.deserialize(serialized);

        assertThat(deserialized).isEqualTo(source);
    }

    @Test
    void securitySerializer_payloadIsNotReadableByCoreFallbackSerializer() {
        MemberPrincipal source = new MemberPrincipal(
                1L,
                "member@stockwellness.org",
                "chan",
                LoginType.KAKAO,
                MemberRole.USER,
                List.of(new SimpleGrantedAuthority("USER")),
                Map.of("provider", "kakao")
        );

        byte[] serialized = apiSecuritySerializer.serialize(source);

        assertThatThrownBy(() -> coreFallbackSerializer.deserialize(serialized))
                .isInstanceOf(SerializationException.class);
    }

    @Test
    void securitySerializer_payloadIsReadableBySecuritySerializer() {
        MemberPrincipal source = new MemberPrincipal(
                1L,
                "member@stockwellness.org",
                "chan",
                LoginType.KAKAO,
                MemberRole.USER,
                List.of(new SimpleGrantedAuthority("USER")),
                Map.of("provider", "kakao")
        );

        byte[] serialized = apiSecuritySerializer.serialize(source);

        assertThatCode(() -> apiSecuritySerializer.deserialize(serialized))
                .doesNotThrowAnyException();
        assertThat(apiSecuritySerializer.deserialize(serialized)).isEqualTo(source);
    }

    private record SampleCacheValue(
            String keyword,
            LocalDateTime createdAt,
            List<String> tags
    ) {
    }

    private record SimpleCacheValue(
            String keyword,
            List<String> tags
    ) {
    }
}
