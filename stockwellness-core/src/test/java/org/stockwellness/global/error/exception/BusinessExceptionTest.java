package org.stockwellness.global.error.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.global.error.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    static class TestBusinessException extends BusinessException {
        public TestBusinessException(ErrorCode errorCode) {
            super(errorCode);
        }
    }

    @Test
    @DisplayName("BusinessException은 ErrorCode를 가져야 한다")
    void businessException_has_errorCode_test() {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        BusinessException exception = new TestBusinessException(errorCode);

        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
    }
}
