package org.stockwellness.global.error.exception;

import lombok.Getter;
import org.springframework.http.ProblemDetail;
import org.stockwellness.global.error.ErrorCode;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final ProblemDetail problemDetail;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.problemDetail = ProblemDetail.forStatusAndDetail(
                errorCode.getStatus(), errorCode.getMessage());
        this.problemDetail.setTitle(errorCode.name());
        this.problemDetail.setProperty("errorCode", errorCode.name());
    }

}