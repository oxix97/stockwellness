package org.stockwellness.adapter.out.external.kis.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RateLimiterConfigTest {

    @Autowired
    private RateLimiter kisRateLimiter;

    @Test
    @DisplayName("테스트 환경에서 kisRateLimiter의 timeoutDuration은 0ms여야 한다")
    void kisRateLimiter_timeoutDuration_shouldBeZeroInTest() {
        assertThat(kisRateLimiter.getRateLimiterConfig().getTimeoutDuration().toMillis())
                .isEqualTo(0L);
    }
}
