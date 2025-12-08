package org.stockwellness.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.in.web.auth.dto.LoginRequest;
import org.stockwellness.adapter.in.web.auth.dto.LoginResponse;
import org.stockwellness.adapter.in.web.auth.dto.ReissueResponse;
import org.stockwellness.adapter.out.external.jwt.JwtProvider;
import org.stockwellness.application.port.out.RefreshTokenPort;
import org.stockwellness.domain.auth.RefreshToken;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.MemberRepository;
import org.stockwellness.domain.shared.Email;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class AuthService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenPort refreshTokenPort;

    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmailAndLoginType(new Email(request.email()), request.loginType())
                .orElseGet(() -> {
                    Member newMember = Member.register(
                            request.email(),
                            request.nickname(),
                            request.loginType()
                    );
                    return memberRepository.save(newMember);
                });

        String accessToken = jwtProvider.generateAccessToken(member);
        String refreshToken = jwtProvider.generateRefreshToken(member);

        LocalDateTime expiredAt = LocalDateTime.now().plusDays(30); // jwtProperties에서 가져오게 수정 가능
        RefreshToken rt = RefreshToken.create(member.getId(), refreshToken, expiredAt);
        refreshTokenPort.save(rt);

        return new LoginResponse(accessToken, refreshToken, member.getId(), member.getEmail().address(), member.getNickname());
    }

    public ReissueResponse reissue(String oldRefreshToken) {
        // 1. 토큰 유효성
        if (!jwtProvider.isTokenValid(oldRefreshToken)) {
            throw new BusinessException(ErrorCode.EXPIRED_JWT);
        }

        Long memberId = jwtProvider.extractMemberId(oldRefreshToken);
        RefreshToken stored = refreshTokenPort.findByMemberId(memberId);
        if (stored == null || !stored.tokenValue().equals(oldRefreshToken) || stored.isExpired()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 2. Rotation
        refreshTokenPort.deleteByMemberId(memberId);

        String newAccessToken = jwtProvider.generateAccessToken(member);
        String newRefreshToken = jwtProvider.generateRefreshToken(member);
        LocalDateTime newExpiredAt = LocalDateTime.now().plusDays(30);

        refreshTokenPort.save(RefreshToken.create(memberId, newRefreshToken, newExpiredAt));

        return new ReissueResponse(newAccessToken, newRefreshToken);
    }

    public void logout(Long memberId) {
        refreshTokenPort.deleteByMemberId(memberId);
    }
}
