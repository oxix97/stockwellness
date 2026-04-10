package org.stockwellness.adapter.out.external.slack;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.stockwellness.application.port.out.notification.NotificationPort;

import java.util.Map;

/**
 * Slack Webhook을 사용하여 외부 알림을 전송하는 어댑터
 */
@Slf4j
@Component
public class SlackNotificationAdapter implements NotificationPort {

    private final RestClient restClient;
    private final String webhookUrl;

    public SlackNotificationAdapter(
        RestClient.Builder restClientBuilder,
        @Value("${slack.webhook.url}") String webhookUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void send(String title, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Slack Webhook URL이 설정되지 않았습니다. 알림을 건너뜁니다: {} - {}", title, content);
            return;
        }

        try {
            String message = String.format("*[%s]*\n%s", title, content);
            Map<String, String> payload = Map.of("text", message);
            
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            
            log.info("Slack 알림 전송 성공: {}", title);
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패: {}", e.getMessage(), e);
            // 알림 실패가 전체 비즈니스 로직(배치)을 중단시키지 않도록 예외를 던지지 않고 로깅만 수행
        }
    }
}
