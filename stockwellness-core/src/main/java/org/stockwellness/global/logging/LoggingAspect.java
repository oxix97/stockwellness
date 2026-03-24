package org.stockwellness.global.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * 전역 로깅 Aspect.
 * {@link LogExecution} 어노테이션 또는 네이밍 컨벤션으로 대상을 감지하고,
 * 메서드 실행 정보를 구조화된 JSON으로 로깅합니다.
 *
 * <p>민감 데이터 마스킹은 {@link MaskingObjectMapper}에 위임합니다.</p>
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);
    private final ObjectMapper objectMapper = MaskingObjectMapper.create();

    @Pointcut("@within(org.stockwellness.global.logging.LogExecution) || " +
            "@annotation(org.stockwellness.global.logging.LogExecution) || " +
            "within(org.stockwellness..*Service) || " +
            "within(org.stockwellness..*Controller) || " +
            "within(org.stockwellness..*Adapter*)")
    public void logExecutionTarget() {
    }

    @Around("logExecutionTarget()")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        Object result = null;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - start;

            LogEvent event = new LogEvent(
                    MDC.get("traceId"),
                    className,
                    methodName,
                    joinPoint.getArgs(),
                    exception == null ? result : null,
                    executionTime,
                    exception != null ? exception.getClass().getSimpleName() : null,
                    exception != null ? exception.getMessage() : null
            );

            writeLog(event);
        }
    }

    private void writeLog(LogEvent event) {
        try {
            if (log.isInfoEnabled()) {
                log.info(objectMapper.writeValueAsString(event));
            }
        } catch (JsonProcessingException e) {
            log.warn("로그 이벤트 JSON 직렬화 실패: {}", e.getMessage());
        }
    }
}
