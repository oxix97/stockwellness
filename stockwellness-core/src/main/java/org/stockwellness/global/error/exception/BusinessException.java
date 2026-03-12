package org.stockwellness.global.error.exception;

import lombok.Getter;
import org.springframework.http.ProblemDetail;
import org.stockwellness.global.error.ErrorCode;

@Getter
public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}