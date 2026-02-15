package org.stockwellness.application.port.in.auth.result;

public record LoginResult(
    String accessToken,
    String refreshToken,
    Long memberId,
    String email,
    String nickname
) {}
