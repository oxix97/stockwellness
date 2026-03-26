package org.stockwellness.global.error;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.stockwellness.global.alert.SlackAlertService;
import org.stockwellness.global.error.exception.BusinessException;

import org.stockwellness.global.common.response.ApiResponse;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final SlackAlertService slackAlertService;

    /**
     * 표준 예외 처리 통합 핸들러
     */
    @ExceptionHandler({
            BusinessException.class,
            MethodArgumentNotValidException.class,
            ExpiredJwtException.class,
            JwtException.class,
            NoResourceFoundException.class,
            Exception.class
    })
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(Exception e) {
        String traceId = generateTraceId();

        return switch (e) {
            case BusinessException be -> handleBusinessException(be, traceId);
            case MethodArgumentNotValidException me -> handleBindingException(me, traceId);
            case ExpiredJwtException ignored -> createErrorResponse(ErrorCode.EXPIRED_JWT, traceId);
            case JwtException ignored -> createErrorResponse(ErrorCode.INVALID_JWT, traceId);
            case NoResourceFoundException ignored -> createErrorResponse(ErrorCode.RESOURCE_NOT_FOUND, traceId);
            default -> handleUnexpectedException(e, traceId);
        };
    }

    private ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, String traceId) {
        log.warn("[{}] BusinessException: {}", traceId, e.getMessage());
        return createErrorResponse(e.getErrorCode(), traceId);
    }

    private ResponseEntity<ApiResponse<Void>> handleBindingException(MethodArgumentNotValidException e, String traceId) {
        log.warn("[{}] MethodArgumentNotValidException: {}", traceId, e.getMessage());
        List<ApiResponse.FieldError> fieldErrors = getFieldErrors(e.getBindingResult());
        return createErrorResponse(ErrorCode.INVALID_INPUT_VALUE, traceId, fieldErrors);
    }

    private ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e, String traceId) {
        log.error("[{}] Unexpected Exception: ", traceId, e);
        slackAlertService.sendInternalServerErrorAlert(traceId, e);
        return createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, traceId);
    }

    private ResponseEntity<ApiResponse<Void>> createErrorResponse(ErrorCode errorCode, String traceId) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode, traceId));
    }

    private ResponseEntity<ApiResponse<Void>> createErrorResponse(ErrorCode errorCode, String traceId, List<ApiResponse.FieldError> fieldErrors) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode, traceId, fieldErrors));
    }

    private List<ApiResponse.FieldError> getFieldErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> new ApiResponse.FieldError(
                        error.getField(),
                        error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                        error.getDefaultMessage()))
                .toList();
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
