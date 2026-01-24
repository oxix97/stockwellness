package org.stockwellness.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.auth.AuthUseCase;
import org.stockwellness.application.port.in.auth.command.LoginCommand;
import org.stockwellness.application.port.in.auth.result.LoginResult;
import org.stockwellness.application.port.in.auth.result.ReissueResult;
import org.stockwellness.adapter.out.external.jwt.JwtProvider;
import org.stockwellness.application.port.out.RefreshTokenPort;
import org.stockwellness.application.port.out.member.LoadMemberPort;
import org.stockwellness.application.port.out.member.SaveMemberPort;
import org.stockwellness.domain.auth.RefreshToken;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.shared.Email;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class AuthService implements AuthUseCase {
    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final JwtProvider jwtProvider;
    private final RefreshTokenPort refreshTokenPort;

    @Override
    public LoginResult login(LoginCommand command) {
        Member member = loadMemberPort.loadMemberByEmailAndLoginType(new Email(command.email()), command.loginType())
                .orElseGet(() -> {
                    Member newMember = Member.register(
                            command.email(),
                            command.nickname(),
                            command.loginType()
                    );
                    return saveMemberPort.saveMember(newMember);
                });

        String accessToken = jwtProvider.generateAccessToken(member);
        String refreshToken = jwtProvider.generateRefreshToken(member);

        LocalDateTime expiredAt = LocalDateTime.now().plusDays(30); // jwtProperties에서 가져오게 수정 가능
        RefreshToken rt = RefreshToken.create(member.getId(), refreshToken, expiredAt);
        refreshTokenPort.save(rt);

        return new LoginResult(accessToken, refreshToken, member.getId(), member.getEmail().address(), member.getNickname());
    }

    @Override
    public ReissueResult reissue(String oldRefreshToken) {
        // 1. 토큰 유효성
        if (!jwtProvider.isTokenValid(oldRefreshToken)) {
            throw new BusinessException(ErrorCode.EXPIRED_JWT);
        }

        Long memberId = jwtProvider.extractMemberId(oldRefreshToken);
        RefreshToken stored = refreshTokenPort.findByMemberId(memberId);
        if (stored == null || !stored.tokenValue().equals(oldRefreshToken) || stored.isExpired()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = loadMemberPort.loadMember(memberId)
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

        return new ReissueResult(newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(Long memberId) {
        refreshTokenPort.deleteByMemberId(memberId);
    }
}