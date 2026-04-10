package org.stockwellness.global.config;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClientException;
import org.stockwellness.adapter.out.external.kis.exception.KisApiException;
import org.stockwellness.adapter.out.external.kis.exception.KisAuthenticationException;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

    @Bean
    public RetryRegistry retryRegistry() {
        return RetryRegistry.of(kisRetryConfig());
    }

    @Bean
    public RateLimiter kisRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(10)
                .timeoutDuration(Duration.ofSeconds(20))
                .build();
        log.info("[KIS 설정] RateLimiter 초기화 limitForPeriod={}, refreshPeriod={}, timeout={}",
                config.getLimitForPeriod(), config.getLimitRefreshPeriod(), config.getTimeoutDuration());
        return RateLimiterRegistry.of(config).rateLimiter("kisRateLimiter");
    }

    public static RetryConfig kisRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(500, 1.5))
                .retryOnException(throwable -> {
                    if (throwable instanceof RestClientException) return true;
                    if (throwable instanceof KisAuthenticationException) return true;
                    if (throwable instanceof KisApiException kisApiException) {
                        return kisApiException.isRateLimitExceeded();
                    }
                    return false;
                })
                .build();
    }
}
