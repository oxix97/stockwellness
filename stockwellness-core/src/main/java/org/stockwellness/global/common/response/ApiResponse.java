package org.stockwellness.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.stockwellness.global.error.ErrorCode;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 프로젝트 전체의 표준 API 응답 포맷입니다.
 * 성공과 에러를 모두 아우르며, Java 21의 record를 사용하여 불변성을 보장합니다.
 *
 * @param <T>         응답 데이터의 타입
 * @param isSuccess   성공 여부 (JSON에서는 success로 노출)
 * @param status      HTTP 상태 코드
 * @param code        비즈니스 상세 코드
 * @param message     사용자에게 전달할 메시지
 * @param data        성공 시 반환할 데이터 (에러 시 null)
 * @param timestamp   응답 생성 시간
 * @param traceId     에러 추적을 위한 ID (성공 시 null 가능)
 * @param errors      상세 필드 에러 정보 (성공 시 빈 리스트)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        @JsonProperty("success")
        boolean isSuccess,
        int status,
        String code,
        String message,
        T data,
        LocalDateTime timestamp,
        String traceId,
        List<FieldError> errors
) {

    /**
     * 성공 응답 생성 (SuccessCode 기반)
     */
    public static <T> ApiResponse<T> success(SuccessCode successCode, T data) {
        return new ApiResponse<>(
                true,
                successCode.getStatusCode(),
                successCode.getCode(),
                successCode.getMessage(),
                data,
                LocalDateTime.now(),
                null,
                Collections.emptyList()
        );
    }

    /**
     * 성공 응답 생성 (기본 SUCCESS 코드 사용)
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(SuccessCode.OK, data);
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     */
    public static <T> ApiResponse<Void> success() {
        return success(SuccessCode.OK, null);
    }

    /**
     * 에러 응답 생성 (ErrorCode 기반)
     */
    public static ApiResponse<Void> error(ErrorCode errorCode, String traceId) {
        return new ApiResponse<>(
                false,
                errorCode.getStatusCode(),
                errorCode.getCode(),
                errorCode.getMessage(),
                null,
                LocalDateTime.now(),
                traceId,
                Collections.emptyList()
        );
    }

    /**
     * 에러 응답 생성 (필드 에러 포함)
     */
    public static ApiResponse<Void> error(ErrorCode errorCode, String traceId, List<FieldError> errors) {
        return new ApiResponse<>(
                false,
                errorCode.getStatusCode(),
                errorCode.getCode(),
                errorCode.getMessage(),
                null,
                LocalDateTime.now(),
                traceId,
                errors
        );
    }

    /**
     * 상세 필드 에러 정보를 담는 record (기존 ErrorResponse.FieldError 유지)
     */
    public record FieldError(
            String field,
            String value,
            String reason
    ) {
    }
}
