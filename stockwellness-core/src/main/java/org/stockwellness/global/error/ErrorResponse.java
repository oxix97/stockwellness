package org.stockwellness.global.error;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 표준 API 에러 응답 포맷
 */
public record ErrorResponse(
        int status,
        String code,
        String message,
        LocalDateTime timestamp,
        String traceId,
        List<FieldError> errors
) {
    public static ErrorResponse of(ErrorCode errorCode, String traceId) {
        return new ErrorResponse(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                LocalDateTime.now(),
                traceId,
                Collections.emptyList()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String traceId, List<FieldError> errors) {
        return new ErrorResponse(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                LocalDateTime.now(),
                traceId,
                errors
        );
    }

    public record FieldError(
            String field,
            String value,
            String reason
    ) {
    }
}
