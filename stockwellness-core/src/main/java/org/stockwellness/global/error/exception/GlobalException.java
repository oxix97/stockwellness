package org.stockwellness.global.error.exception;

import org.stockwellness.global.error.ErrorCode;

/**
 * 전역 비즈니스 예외 (구체 클래스)
 */
public class GlobalException extends BusinessException {
    public GlobalException(ErrorCode errorCode) {
        super(errorCode);
    }
}
