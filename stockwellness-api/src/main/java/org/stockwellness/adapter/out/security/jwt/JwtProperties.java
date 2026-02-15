package org.stockwellness.adapter.out.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secretKey,
        Long accessTokenExpiryMs,
        Long refreshTokenExpiryMs
) {
}
