package org.stockwellness.adapter.out.external.slack;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.notification.NotificationPort;
import org.stockwellness.global.alert.NotificationContext;
import org.stockwellness.global.alert.SlackNotificationService;

/**
 * Slack Webhook을 사용하여 외부 알림을 전송하는 어댑터.
 * 이제 공통 SlackNotificationService를 사용하여 일관된 포맷으로 알림을 전송합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackNotificationAdapter implements NotificationPort {

    private final SlackNotificationService slackNotificationService;

    @Override
    public void send(String title, String content) {
        try {
            NotificationContext.NotificationType type = determineType(title);
            Map<String, String> details = extractDetails(content);

            NotificationContext context = NotificationContext.builder()
                    .title(title)
                    .content(content)
                    .type(type)
                    .details(details)
                    .build();

            slackNotificationService.sendNotification(context);
        } catch (Exception e) {
            log.error("Slack 알림 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private NotificationContext.NotificationType determineType(String title) {
        if (title.contains("실패") || title.contains("오류") || 
            title.contains("Failure") || title.contains("Error") ||
            title.contains("FAILED")) {
            return NotificationContext.NotificationType.ERROR;
        }
        return NotificationContext.NotificationType.SUCCESS;
    }

    private Map<String, String> extractDetails(String content) {
        Map<String, String> details = new LinkedHashMap<>();
        if (content == null || content.isBlank()) {
            return details;
        }

        content.lines().forEach(line -> {
            int colonIndex = line.indexOf(':');
            // "Key: Value" 형태이면서 Key가 한 단어인 경우만 details로 추출 (배치 통계 등)
            if (colonIndex > 0 && colonIndex < line.length() - 1) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();

                // Key가 너무 길거나 공백이 많으면 일반 텍스트로 간주 (방어 로직)
                if (key.length() < 20 && !key.contains(" ")) {
                    details.put(key, value);
                }
            }
        });
        return details;
    }
}

