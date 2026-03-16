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
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Global logging aspect triggered by @LogExecution annotation.
 * Logs are output in structured JSON format with deep masking.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MASKED_VALUE = "********";
    private static final Set<String> SENSITIVE_KEYWORDS = new HashSet<>(Arrays.asList(
            "password", "pwd", "accessToken", "refreshToken", "token", "secret", "authorization"
    ));

    /**
     * Targets classes marked with @LogExecution or methods marked with @LogExecution.
     * Also targets Service, Controller, and Adapter layers by naming convention.
     */
    @Pointcut("@within(org.stockwellness.global.logging.LogExecution) || " +
            "@annotation(org.stockwellness.global.logging.LogExecution) || " +
            "within(org.stockwellness..*Service) || " +
            "within(org.stockwellness..*Controller) || " +
            "within(org.stockwellness..*Adapter*)")
    public void logExecutionTarget() {}

    @Around("logExecutionTarget()")
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
                    .traceId(MDC.get("traceId"))
                    .className(className)
                    .methodName(methodName)
                    .args(maskSensitiveData(args))
                    .result(maskSensitiveData(result))
                    .executionTimeMs(executionTime)
                    .exceptionMessage(exception != null ? exception.getMessage() : null)
                    .stackTrace(exception != null ? getStackTrace(exception) : null)
                    .build();

            try {
                if (log.isInfoEnabled()) {
                    log.info(objectMapper.writeValueAsString(logEvent));
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize log event to JSON: {}", e.getMessage());
            }
        }
    }

    private Object maskSensitiveData(Object data) {
        if (data == null) return null;

        if (data instanceof Object[] objects) {
            return Arrays.stream(objects)
                    .map(this::processObject)
                    .collect(Collectors.toList());
        }

        if (data instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::processObject)
                    .collect(Collectors.toList());
        }

        if (data instanceof Map<?, ?> map) {
            Map<Object, Object> maskedMap = new HashMap<>();
            map.forEach((k, v) -> {
                if (k instanceof String key && isSensitiveKey(key)) {
                    maskedMap.put(k, MASKED_VALUE);
                } else {
                    maskedMap.put(k, processObject(v));
                }
            });
            return maskedMap;
        }

        return processObject(data);
    }

    private Object processObject(Object obj) {
        if (obj == null) return null;

        if (isSimpleType(obj)) {
            return obj;
        }

        if (obj.getClass().getName().startsWith("org.stockwellness")) {
            return performDeepMasking(obj);
        }

        return obj;
    }

    private boolean isSimpleType(Object obj) {
        return obj instanceof String || obj instanceof Number || obj instanceof Boolean ||
               obj instanceof Character || obj.getClass().isPrimitive() || obj.getClass().isEnum();
    }

    private Object performDeepMasking(Object obj) {
        try {
            Map<String, Object> maskedFields = new HashMap<>();
            Class<?> clazz = obj.getClass();

            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    Object fieldValue = field.get(obj);

                    if (field.isAnnotationPresent(Masked.class) || isSensitiveKey(fieldName)) {
                        maskedFields.put(fieldName, MASKED_VALUE);
                    } else if (fieldValue != null && field.getType().getName().startsWith("org.stockwellness")) {
                        maskedFields.put(fieldName, processObject(fieldValue));
                    } else {
                        maskedFields.put(fieldName, fieldValue);
                    }
                }
                clazz = clazz.getSuperclass();
            }
            return maskedFields;
        } catch (Exception e) {
            return "[Masking Error: " + e.getMessage() + "]";
        }
    }

    private boolean isSensitiveKey(String key) {
        String lower = key.toLowerCase();
        return SENSITIVE_KEYWORDS.stream().anyMatch(lower::contains);
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
        private String traceId;
        private String className;
        private String methodName;
        private Object args;
        private Object result;
        private Long executionTimeMs;
        private String exceptionMessage;
        private String stackTrace;
    }
}
