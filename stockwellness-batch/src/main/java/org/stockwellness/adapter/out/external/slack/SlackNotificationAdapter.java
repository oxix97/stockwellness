package org.stockwellness.adapter.out.external.slack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.stockwellness.application.port.out.notification.NotificationPort;

import java.util.Map;

@Component
public class SlackNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(SlackNotificationAdapter.class);

    private final RestTemplate restTemplate;
    private final String webhookUrl;

    public SlackNotificationAdapter(
        RestTemplate restTemplate,
        @Value("${slack.webhook.url:}") String webhookUrl
    ) {
        this.restTemplate = restTemplate;
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void send(String title, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Slack Webhook URL is not configured. Notification skipped: {} - {}", title, content);
            return;
        }

        try {
            String message = String.format("*[%s]*\n%s", title, content);
            Map<String, String> payload = Map.of("text", message);
            
            restTemplate.postForEntity(webhookUrl, payload, String.class);
            log.info("Successfully sent Slack notification: {}", title);
        } catch (Exception e) {
            log.error("Failed to send Slack notification: {}", e.getMessage(), e);
        }
    }
}
