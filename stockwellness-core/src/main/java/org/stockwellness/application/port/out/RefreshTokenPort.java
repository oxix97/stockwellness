package org.stockwellness.application.port.out;

import org.stockwellness.domain.auth.RefreshToken;

public interface RefreshTokenPort {
    void save(RefreshToken refreshToken);
    RefreshToken findByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
}