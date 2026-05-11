package org.stockwellness.global.config;

import java.util.concurrent.atomic.AtomicInteger;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.stockwellness.adapter.out.external.kis.config.ResilienceConfig;
import org.stockwellness.adapter.out.external.kis.exception.KisApiException;
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

    @Test
    @DisplayName("KIS 재조회 권장 업무 오류는 설정된 횟수만큼 재시도한다")
    void retryRegistry_retriesRetryableBusinessExceptions() {
        Retry retry = retryRegistry.retry("kisRetry-business");
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> Retry.decorateRunnable(retry, () -> {
            attempts.incrementAndGet();
            throw new KisApiException("1", "EGW00316", "조회 처리 중 오류 발생하였습니다. 재 조회 수행 부탁드립니다.");
        }).run()).isInstanceOf(KisApiException.class);

        assertThat(attempts).hasValue(3);
    }

    @Test
    @DisplayName("일반 업무 오류는 재시도하지 않는다")
    void retryRegistry_doesNotRetryNonRetryableBusinessExceptions() {
        Retry retry = retryRegistry.retry("kisRetry-non-retryable");
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> Retry.decorateRunnable(retry, () -> {
            attempts.incrementAndGet();
            throw new KisApiException("1", "ABC12345", "유효하지 않은 종목 코드입니다.");
        }).run()).isInstanceOf(KisApiException.class);

        assertThat(attempts).hasValue(1);
    }
}
