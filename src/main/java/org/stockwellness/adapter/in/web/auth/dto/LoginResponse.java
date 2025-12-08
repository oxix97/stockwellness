package org.stockwellness.adapter.in.web.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long memberId,
        String email,
        String nickname
) {}