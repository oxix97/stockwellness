package org.stockwellness.adapter.out.persistence.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.auth.RefreshTokenPort;
import org.stockwellness.domain.auth.RefreshToken;
import org.stockwellness.global.util.DateUtil;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RefreshTokenRedisAdapter implements RefreshTokenPort {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "refresh_token:";

    private String getKey(Long memberId) {
        return KEY_PREFIX + memberId;
    }

    @Override
    public void save(RefreshToken refreshToken) {
        String key = getKey(refreshToken.memberId());
        String value = refreshToken.tokenValue() + "::" + refreshToken.expiredAt(); // 간단 직렬화
        Duration ttl = DateUtil.durationBetween(DateUtil.now(), refreshToken.expiredAt());
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public RefreshToken findByMemberId(Long memberId) {
        String key = getKey(memberId);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        String[] parts = value.split("::");
        String tokenValue = parts[0];
        LocalDateTime expiredAt = LocalDateTime.parse(parts[1]);
        return RefreshToken.create(memberId, tokenValue, expiredAt);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        String key = getKey(memberId);
        redisTemplate.delete(key);
    }
}
