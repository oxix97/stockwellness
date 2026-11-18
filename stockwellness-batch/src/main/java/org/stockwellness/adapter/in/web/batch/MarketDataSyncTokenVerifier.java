package org.stockwellness.adapter.in.web.batch;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.GlobalException;

@Component
public class MarketDataSyncTokenVerifier {

    private final String configuredToken;

    public MarketDataSyncTokenVerifier(
            @Value("${stockwellness.batch.market-data.sync-token:}") String configuredToken) {
        this.configuredToken = configuredToken;
    }

    public void verify(String suppliedToken) {
        if (!StringUtils.hasText(configuredToken) || !StringUtils.hasText(suppliedToken)
                || !MessageDigest.isEqual(
                        configuredToken.getBytes(StandardCharsets.UTF_8),
                        suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new GlobalException(ErrorCode.UNAUTHORIZED);
        }
    }
}
