package org.stockwellness.adapter.out.external.kis.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Component
public class KisTokenAdapter {

    private final RestClient authClient;
    private final KisProperties kisProperties;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_TOKEN_KEY = "stockwellness:kis:token";
    private static final long SAFETY_MARGIN_SECONDS = 300;

    public KisTokenAdapter(
            @Qualifier("kisAuthClient") RestClient authClient,
            KisProperties kisProperties,
            StringRedisTemplate redisTemplate
    ) {
        this.authClient = authClient;
        this.kisProperties = kisProperties;
        this.redisTemplate = redisTemplate;
    }

    public String getAccessToken() {
        String cachedToken = redisTemplate.opsForValue().get(REDIS_TOKEN_KEY);
        if (cachedToken != null) {
            return cachedToken;
        }

        // 2. 동시성 제어 (배치 환경 고려 synchronized 사용)
        synchronized (this) {
            cachedToken = redisTemplate.opsForValue().get(REDIS_TOKEN_KEY);
            if (cachedToken != null) {
                return cachedToken;
            }

            log.info("KIS Access Token이 만료되어 재발급을 시도합니다.");
            return refreshAccessToken();
        }
    }

    private String refreshAccessToken() {
        var request = new KisTokenRequest(
                "client_credentials",
                kisProperties.appKey(),
                kisProperties.appSecret()
        );

        KisTokenResponse response = authClient.post()
                .uri("/oauth2/tokenP")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(KisTokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new RuntimeException("KIS API 토큰 발급 실패: 응답이 비어있습니다.");
        }

        // 4. Redis 저장
        long ttl = response.expiresIn() - SAFETY_MARGIN_SECONDS;
        redisTemplate.opsForValue().set(REDIS_TOKEN_KEY, response.accessToken(), Duration.ofSeconds(ttl));

        log.info("KIS Access Token 재발급 완료. TTL: {}초", ttl);
        return response.accessToken();
    }

    record KisTokenRequest(
            String grant_type,
            String appkey,
            String appsecret
    ) {
    }
    record KisTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("token_type") String tokenType
    ) {
    }
}