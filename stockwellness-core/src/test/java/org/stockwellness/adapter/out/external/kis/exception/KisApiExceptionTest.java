package org.stockwellness.adapter.out.external.kis.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class KisApiExceptionTest {

    @Test
    @DisplayName("초당 제한 메시지는 rate limit 오류로 판별한다")
    void detectsRateLimitExceeded() {
        KisApiException exception = new KisApiException("1", null, "초당 거래건수를 초과하였습니다.");

        assertThat(exception.isRateLimitExceeded()).isTrue();
        assertThat(exception.isRetryableBusinessError()).isFalse();
        assertThat(exception.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("EGW00316 코드는 재시도 가능한 업무 오류로 판별한다")
    void detectsRetryableBusinessErrorByCode() {
        KisApiException exception = new KisApiException("1", "EGW00316", "일시 오류");

        assertThat(exception.isRetryableBusinessError()).isTrue();
        assertThat(exception.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("재조회 권장 메시지는 재시도 가능한 업무 오류로 판별한다")
    void detectsRetryableBusinessErrorByMessage() {
        KisApiException exception = new KisApiException("1", null, "조회 처리 중 오류 발생하였습니다. 재 조회 수행 부탁드립니다.");

        assertThat(exception.isRetryableBusinessError()).isTrue();
        assertThat(exception.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("일반 업무 오류는 재시도 대상으로 판별하지 않는다")
    void ignoresNonRetryableBusinessError() {
        KisApiException exception = new KisApiException("1", "ABC12345", "유효하지 않은 요청입니다.");

        assertThat(exception.isRateLimitExceeded()).isFalse();
        assertThat(exception.isRetryableBusinessError()).isFalse();
        assertThat(exception.isRetryable()).isFalse();
    }
}
