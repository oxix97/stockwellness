package org.stockwellness.domain.auth;

import org.stockwellness.global.util.DateUtil;

import java.time.LocalDateTime;

public record RefreshToken(
        Long memberId,
        String tokenValue,
        LocalDateTime issuedAt,
        LocalDateTime expiredAt
) {
    public static RefreshToken create(Long memberId, String tokenValue, LocalDateTime expiredAt) {
        return new RefreshToken(memberId, tokenValue, DateUtil.now(), expiredAt);
    }

    public boolean isExpired() {
        return DateUtil.isExpired(expiredAt);
    }
}
