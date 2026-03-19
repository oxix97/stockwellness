package org.stockwellness.global.logging;

import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * 로그 이벤트 데이터를 담는 불변 DTO.
 * AOP Aspect에서 생성되어 JSON으로 직렬화됩니다.
 *
 * @param traceId          요청 추적 ID (MDC)
 * @param className        대상 클래스명
 * @param methodName       대상 메서드명
 * @param args             메서드 인자
 * @param result           메서드 반환값
 * @param executionTimeMs  실행 시간 (ms)
 * @param exceptionClass   예외 클래스명 (예외 발생 시)
 * @param exceptionMessage 예외 메시지 (예외 발생 시)
 */
@JsonInclude(NON_NULL)
public record LogEvent(
        String traceId,
        String className,
        String methodName,
        Object args,
        Object result,
        Long executionTimeMs,
        String exceptionClass,
        String exceptionMessage
) {
}
