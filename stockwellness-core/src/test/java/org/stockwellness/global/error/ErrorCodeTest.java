package org.stockwellness.global.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    @DisplayName("ErrorCode는 HTTP 상태, 커스텀 코드, 기본 메시지를 가져야 한다")
    void errorCode_fields_test() {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(errorCode.getCode()).isNotNull();
        assertThat(errorCode.getMessage()).isNotNull();
    }
}
