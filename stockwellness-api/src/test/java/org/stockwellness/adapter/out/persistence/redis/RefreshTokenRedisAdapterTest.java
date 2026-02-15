package org.stockwellness.adapter.out.persistence.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.stockwellness.domain.auth.RefreshToken;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenRedisAdapterTest {

    @Autowired
    private RefreshTokenRedisAdapter adapter;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    @DisplayName("로그인 시 기존에 만료되지 않은 토큰이 있어도 새로운 토큰으로 정상적으로 덮어써진다")
    void save_overwrites_existing_token() {
        // given: 기존 토큰 저장
        Long memberId = 1L;
        String oldToken = "old-token-value";
        LocalDateTime oldExpiry = LocalDateTime.now().plusDays(1);
        RefreshToken oldRt = RefreshToken.create(memberId, oldToken, oldExpiry);
        adapter.save(oldRt);

        // when: 새로운 토큰 저장 (로그인 상황 재현)
        String newToken = "new-token-value";
        LocalDateTime newExpiry = LocalDateTime.now().plusDays(7);
        RefreshToken newRt = RefreshToken.create(memberId, newToken, newExpiry);
        adapter.save(newRt);

        // then: 조회 시 새로운 토큰이어야 함
        RefreshToken found = adapter.findByMemberId(memberId);
        assertThat(found.tokenValue()).isEqualTo(newToken);
        assertThat(found.tokenValue()).isNotEqualTo(oldToken);
    }
}
