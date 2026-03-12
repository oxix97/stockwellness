package org.stockwellness.global.error;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.stockwellness.global.error.exception.BusinessException;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception e) {
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

    private ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, String traceId) {
        log.warn("[{}] BusinessException: {}", traceId, e.getMessage());
        return createErrorResponse(e.getErrorCode(), traceId);
    }

    private ResponseEntity<ErrorResponse> handleBindingException(MethodArgumentNotValidException e, String traceId) {
        log.warn("[{}] MethodArgumentNotValidException: {}", traceId, e.getMessage());
        List<ErrorResponse.FieldError> fieldErrors = getFieldErrors(e.getBindingResult());
        return createErrorResponse(ErrorCode.INVALID_INPUT_VALUE, traceId, fieldErrors);
    }

    private ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e, String traceId) {
        log.error("[{}] Unexpected Exception: ", traceId, e);
        return createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, traceId);
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(ErrorCode errorCode, String traceId) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, traceId));
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(ErrorCode errorCode, String traceId, List<ErrorResponse.FieldError> fieldErrors) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, traceId, fieldErrors));
    }

    private List<ErrorResponse.FieldError> getFieldErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(
                        error.getField(),
                        error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                        error.getDefaultMessage()))
                .toList();
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
