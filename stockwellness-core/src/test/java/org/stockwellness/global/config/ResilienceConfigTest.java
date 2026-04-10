package org.stockwellness.global.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.stockwellness.adapter.out.external.kis.exception.KisApiException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResilienceConfigTest {

    private final RetryRegistry retryRegistry = new ResilienceConfig().retryRegistry();

    @Test
    @DisplayName("네트워크 예외는 최대 3회까지 재시도한다")
    void retryRegistry_retriesNetworkExceptions() {
        Retry retry = retryRegistry.retry("kisRetry-network");
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> Retry.decorateRunnable(retry, () -> {
            attempts.incrementAndGet();
            throw new ResourceAccessException("timeout");
        }).run()).isInstanceOf(ResourceAccessException.class);

        assertThat(attempts).hasValue(3);
    }

    @Test
    @DisplayName("KIS 초당 제한 오류는 설정된 횟수만큼 재시도한다")
    void retryRegistry_retriesRateLimitExceptions() {
        Retry retry = retryRegistry.retry("kisRetry-rate-limit");
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> Retry.decorateRunnable(retry, () -> {
            attempts.incrementAndGet();
            throw new KisApiException("1", null, "초당 거래건수를 초과하였습니다.");
        }).run()).isInstanceOf(KisApiException.class);

        assertThat(attempts).hasValue(3);
    }
}
