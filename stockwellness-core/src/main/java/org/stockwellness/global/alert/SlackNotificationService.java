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
        try {
            String webhookUrl = properties.webhookUrl();
            if (webhookUrl == null || webhookUrl.isBlank()) {
                log.warn("[SlackAlert] webhook URL 미설정. 알림 스킵: title={}", context.title());
                return;
            }

            URI uri;
            try {
                if (!webhookUrl.startsWith("http")) {
                    throw new IllegalArgumentException("URL must start with http or https");
                }
                uri = URI.create(webhookUrl);
            } catch (IllegalArgumentException e) {
                log.error("[SlackAlert] 잘못된 webhook URL 형식: {}. 알림 스킵", webhookUrl);
                return;
            }

            Map<String, Object> payload = messageBuilder.build(context);
            restClient.post()
                    .uri(uri)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[SlackAlert] 알림 전송 성공: title={}", context.title());
        } catch (Exception ex) {
            log.error("[SlackAlert] 알림 전송 실패: title={}, reason={}", context.title(), ex.getMessage());
        }
    }
}
