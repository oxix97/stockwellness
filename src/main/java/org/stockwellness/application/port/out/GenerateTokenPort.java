package org.stockwellness.application.port.out;

import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.MemberRole;

public interface GenerateTokenPort {
    String generateAccessToken(Member member);
    String generateRefreshToken(Member member);
    
    // Overloaded methods for MemberPrincipal usage
    String generateAccessToken(Long id, String email, LoginType loginType, MemberRole role);
    String generateRefreshToken(Long id);
}