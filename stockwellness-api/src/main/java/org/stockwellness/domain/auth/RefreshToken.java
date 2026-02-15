package org.stockwellness.domain.auth;

import java.time.LocalDateTime;

public record RefreshToken(
        Long memberId,
        String tokenValue,
        LocalDateTime issuedAt,
        LocalDateTime expiredAt
) {
    public static RefreshToken create(Long memberId, String tokenValue, LocalDateTime expiredAt) {
        return new RefreshToken(memberId, tokenValue, LocalDateTime.now(), expiredAt);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }
}
