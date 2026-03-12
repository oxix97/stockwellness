package org.stockwellness.global.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    @DisplayName("ErrorResponse는 traceId와 FieldError 목록을 포함해야 한다")
    void errorResponse_fields_test() {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        String traceId = "test-trace-id";
        List<ErrorResponse.FieldError> fieldErrors = List.of(
                new ErrorResponse.FieldError("name", "value", "reason")
        );

        ErrorResponse response = ErrorResponse.of(errorCode, traceId, fieldErrors);

        assertThat(response.status()).isEqualTo(errorCode.getStatus().value());
        assertThat(response.code()).isEqualTo(errorCode.getCode());
        assertThat(response.traceId()).isEqualTo(traceId);
        assertThat(response.errors()).hasSize(1);
    }
}
