package org.stockwellness.global.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.global.error.ErrorCode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("성공 응답 객체를 생성한다")
    void success_response_test() {
        // given
        String data = "test data";

        // when
        ApiResponse<String> response = ApiResponse.success(data);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.code()).isEqualTo("S000");
        assertThat(response.message()).isEqualTo("요청이 성공적으로 처리되었습니다.");
        assertThat(response.data()).isEqualTo(data);
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("에러 응답 객체를 생성한다")
    void error_response_test() {
        // given
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        String traceId = "test-trace-id";

        // when
        ApiResponse<Void> response = ApiResponse.error(errorCode, traceId);

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.code()).isEqualTo("G001");
        assertThat(response.message()).isEqualTo("잘못된 입력값입니다.");
        assertThat(response.traceId()).isEqualTo(traceId);
    }

    @Test
    @DisplayName("필드 에러를 포함한 에러 응답 객체를 생성한다")
    void error_with_field_errors_test() {
        // given
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        String traceId = "test-trace-id";
        List<ApiResponse.FieldError> fieldErrors = List.of(
                new ApiResponse.FieldError("email", "invalid", "이메일 형식이 올바르지 않습니다.")
        );

        // when
        ApiResponse<Void> response = ApiResponse.error(errorCode, traceId, fieldErrors);

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().get(0).field()).isEqualTo("email");
    }
}
