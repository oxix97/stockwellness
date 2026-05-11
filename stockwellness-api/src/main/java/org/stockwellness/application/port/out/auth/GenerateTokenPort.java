package org.stockwellness.application.port.out.auth;

import java.time.LocalDateTime;

import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.MemberRole;

public interface GenerateTokenPort {
    String generateAccessToken(Member member);
    String generateRefreshToken(Member member);
    
    // Overloaded methods for MemberPrincipal usage
    String generateAccessToken(Long id, String email, String nickname, LoginType loginType, MemberRole role, LocalDateTime createdAt);
    String generateRefreshToken(Long id);
}