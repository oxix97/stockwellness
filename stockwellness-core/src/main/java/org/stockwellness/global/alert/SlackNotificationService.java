package org.stockwellness.global.alert;

import java.net.URI;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class SlackNotificationService {

    private final SlackAlertProperties properties;
    private final SlackMessageBuilder messageBuilder;
    private final RestClient restClient;

    public SlackNotificationService(
            SlackAlertProperties properties,
            SlackMessageBuilder messageBuilder,
            @Qualifier("slackRestClient") RestClient restClient
    ) {
        this.properties = properties;
        this.messageBuilder = messageBuilder;
        this.restClient = restClient;
    }

    @Async("alertExecutor")
    public void sendNotification(NotificationContext context) {
        if (properties.webhookUrl() == null || properties.webhookUrl().isBlank()) {
            log.warn("[SlackAlert] webhook URL 미설정. 알림 스킵: title={}", context.title());
            return;
        }

        try {
            Map<String, Object> payload = messageBuilder.build(context);
            restClient.post()
                    .uri(URI.create(properties.webhookUrl()))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[SlackAlert] 알림 전송 성공: title={}", context.title());
        } catch (Exception ex) {
            log.error("[SlackAlert] 알림 전송 실패: title={}, reason={}", context.title(), ex.getMessage());
        }
    }
}
