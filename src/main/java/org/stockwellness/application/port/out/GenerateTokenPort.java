package org.stockwellness.application.port.out;

import org.stockwellness.domain.member.Member;

public interface GenerateTokenPort {
    String generateAccessToken(Member member);
    String generateRefreshToken(Member member);
}