package org.stockwellness.global.alert;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackAlertService {

    private static final int MAX_STACK_TRACE_LINES = 5;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private final SlackNotificationService slackNotificationService;

    /**
     * API 시스템 에러 알림 전송 (GlobalExceptionHandler용)
     */
    public void sendErrorAlert(Exception e, String traceId, HttpServletRequest request) {
        String userId = getUserId();
        String url = request.getRequestURI();
        if (request.getQueryString() != null) {
            url += "?" + request.getQueryString();
        }

        NotificationContext context = NotificationContext.builder()
                .title("API 시스템 에러 (500)")
                .content(e.getMessage() != null ? e.getMessage() : "No message available")
                .traceId(traceId)
                .time(ZonedDateTime.now(ZoneId.of("Asia/Seoul")).format(FORMATTER))
                .userId(userId)
                .url(url)
                .exceptionType(e.getClass().getSimpleName())
                .stackTrace(extractStackTrace(e))
                .type(NotificationContext.NotificationType.ERROR)
                .build();

        slackNotificationService.sendNotification(context);
    }

    /**
     * 기존 메서드 호환성 유지용 (필요 시)
     */
    public void sendInternalServerErrorAlert(String traceId, Exception e) {
        NotificationContext context = NotificationContext.builder()
                .title("API 시스템 에러 (500)")
                .content(e.getMessage() != null ? e.getMessage() : "No message available")
                .traceId(traceId)
                .time(ZonedDateTime.now(ZoneId.of("Asia/Seoul")).format(FORMATTER))
                .exceptionType(e.getClass().getSimpleName())
                .stackTrace(extractStackTrace(e))
                .type(NotificationContext.NotificationType.ERROR)
                .build();

        slackNotificationService.sendNotification(context);
    }

    private String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "GUEST";
        }
        return authentication.getName();
    }

    private String extractStackTrace(Exception e) {
        return Arrays.stream(e.getStackTrace())
                .limit(MAX_STACK_TRACE_LINES)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n    at "));
    }
}
