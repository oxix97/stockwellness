package org.stockwellness.adapter.in.web.auth.dto;

public record JwtToken(
        String grantType,
        String accessToken,
        String refreshToken
) {
}
