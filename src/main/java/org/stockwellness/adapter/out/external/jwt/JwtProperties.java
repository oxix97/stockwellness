package org.stockwellness.adapter.out.external.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secretKey,
        Long accessTokenExpiryMs,
        Long refreshTokenExpiryMs
) {
}