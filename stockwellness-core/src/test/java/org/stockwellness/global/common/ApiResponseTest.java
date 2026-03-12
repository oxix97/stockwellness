package org.stockwellness.global.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("성공 응답을 생성하면 데이터와 타임스탬프가 포함되어야 한다")
    void success_test() {
        String data = "test data";
        ApiResponse<String> response = ApiResponse.success(data);

        assertThat(response.data()).isEqualTo(data);
        assertThat(response.timestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
