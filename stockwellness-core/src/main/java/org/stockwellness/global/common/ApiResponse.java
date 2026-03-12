package org.stockwellness.global.common;

import java.time.LocalDateTime;

/**
 * 표준 API 성공 응답 포맷
 */
public record ApiResponse<T>(
        T data,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, LocalDateTime.now());
    }
}
