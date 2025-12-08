package org.stockwellness.global.error;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String code,
        String message,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, ErrorCode errorCode) {
        return new ErrorResponse(status, errorCode.name(), errorCode.getMessage(), LocalDateTime.now());
    }
}
