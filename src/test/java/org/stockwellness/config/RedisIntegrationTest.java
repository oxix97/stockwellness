package org.stockwellness.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.fixture.AuthFixture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Redis 설정 통합 검증 테스트")
class RedisIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("성공: 화이트리스트에 포함된 도메인 객체(Member)를 캐싱하고 조회할 수 있다")
    void member_cache_serialization_test() {
        // Given
        Cache memberCache = cacheManager.getCache("member");
        assertThat(memberCache).isNotNull();

        Long memberId = 1L;
        Member member = AuthFixture.createMember(); // org.stockwellness 패키지 객체

        // When
        memberCache.put(memberId.toString(), member);
        Member cachedMember = memberCache.get(memberId.toString(), Member.class);

        // Then
        assertThat(cachedMember).isNotNull();
        assertThat(cachedMember.getLoginType()).isEqualTo(member.getLoginType());
        assertThat(cachedMember.getEmail().getAddress()).isEqualTo(member.getEmail().getAddress());
        assertThat(cachedMember.getNickname()).isEqualTo(member.getNickname());

        // Clean up
        memberCache.evict(memberId.toString());
    }

    @Test
    @DisplayName("성공: 표준 Java 타입(String) 캐싱이 정상 작동한다")
    void standard_type_cache_test() {
        // Given
        Cache shortLivedCache = cacheManager.getCache("short_lived");
        assertThat(shortLivedCache).isNotNull();

        String key = "test_key";
        String value = "test_value";

        // When
        shortLivedCache.put(key, value);
        String cachedValue = shortLivedCache.get(key, String.class);

        // Then
        assertThat(cachedValue).isEqualTo(value);

        // Clean up
        shortLivedCache.evict(key);
    }
}
