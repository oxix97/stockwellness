package org.stockwellness.global.alert;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slack.alert")
public record SlackAlertProperties(String webhookUrl) {
}
