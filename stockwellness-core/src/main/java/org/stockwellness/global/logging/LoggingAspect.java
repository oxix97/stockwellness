package org.stockwellness.global.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Global logging aspect for method boundary, execution time, and exception logging.
 * Logs are output in structured JSON format.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> SENSITIVE_KEYWORDS = new HashSet<>(Arrays.asList(
            "password", "pwd", "accessToken", "refreshToken", "token", "secret", "authorization"
    ));

    @Pointcut("within(org.stockwellness.application.service..*)")
    public void applicationServiceLayer() {}

    @Pointcut("within(org.stockwellness.adapter.in.web..*)")
    public void webAdapterLayer() {}

    @Pointcut("within(org.stockwellness.batch.job..*)")
    public void batchJobLayer() {}

    @Pointcut("@within(org.stockwellness.global.logging.LogExecution) || @annotation(org.stockwellness.global.logging.LogExecution)")
    public void logExecutionAnnotation() {}

    @Around("applicationServiceLayer() || webAdapterLayer() || batchJobLayer() || logExecutionAnnotation()")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

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
            LogEvent logEvent = LogEvent.builder()
                    .className(className)
                    .methodName(methodName)
                    .args(maskSensitiveData(args))
                    .result(maskSensitiveData(result))
                    .executionTimeMs(executionTime)
                    .exceptionMessage(exception != null ? exception.getMessage() : null)
                    .stackTrace(exception != null ? getStackTrace(exception) : null)
                    .build();

            try {
                log.info(objectMapper.writeValueAsString(logEvent));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize log event to JSON: {}", e.getMessage());
            }
        }
    }

    private Object maskSensitiveData(Object data) {
        if (data == null) return null;
        // Simple masking logic: if it's an array, mask each element if it's a string matching keywords.
        // For complex objects, this is harder without deep reflection.
        // For now, let's just do a basic check for string arguments.
        if (data instanceof Object[] objects) {
            return Arrays.stream(objects)
                    .map(this::maskIfString)
                    .collect(Collectors.toList());
        }
        return maskIfString(data);
    }

    private Object maskIfString(Object obj) {
        if (obj instanceof String s) {
            String lower = s.toLowerCase();
            for (String keyword : SENSITIVE_KEYWORDS) {
                if (lower.contains(keyword.toLowerCase())) {
                    return "********";
                }
            }
        }
        return obj;
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class LogEvent {
        private String className;
        private String methodName;
        private Object args;
        private Object result;
        private Long executionTimeMs;
        private String exceptionMessage;
        private String stackTrace;
    }
}
