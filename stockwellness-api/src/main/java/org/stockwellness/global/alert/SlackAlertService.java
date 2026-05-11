package org.stockwellness.global.alert;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class SlackAlertService {

    private static final int MAX_STACK_TRACE_LINES = 5;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private final SlackAlertProperties properties;
    private final RestClient restClient;

    public SlackAlertService(SlackAlertProperties properties, @Qualifier("slackRestClient") RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Async("alertExecutor")
    public void sendInternalServerErrorAlert(String traceId, Exception e) {
        if (properties.webhookUrl() == null || properties.webhookUrl().isBlank()) {
            log.warn("[SlackAlert] webhook URL 미설정. 알림 스킵: traceId={}", traceId);
            return;
        }

        try {
            Map<String, Object> payload = buildPayload(traceId, e);
            restClient.post()
                    .uri(URI.create(properties.webhookUrl()))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[SlackAlert] 알림 전송 성공: traceId={}", traceId);
        } catch (Exception ex) {
            log.error("[SlackAlert] 알림 전송 실패: traceId={}, reason={}", traceId, ex.getMessage());
        }
    }

    private Map<String, Object> buildPayload(String traceId, Exception e) {
        String stackTrace = Arrays.stream(e.getStackTrace())
                .limit(MAX_STACK_TRACE_LINES)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n    at "));

        String time = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).format(FORMATTER);
        String exceptionType = e.getClass().getSimpleName();
        String exceptionMessage = e.getMessage() != null ? e.getMessage() : "No message available";

        return Map.of(
                "blocks", List.of(
                        Map.of(
                                "type", "header",
                                "text", Map.of("type", "plain_text", "text", "🚨 API 시스템 에러 (500)")
                        ),
                        Map.of(
                                "type", "section",
                                "fields", List.of(
                                        Map.of("type", "mrkdwn", "text", "*Trace ID:*\n`" + traceId + "`"),
                                        Map.of("type", "mrkdwn", "text", "*발생 시각:*\n" + time)
                                )
                        ),
                        Map.of(
                                "type", "section",
                                "text", Map.of("type", "mrkdwn", "text", String.format("*오류 요약:*\n*`%s`*\n> %s", exceptionType, exceptionMessage))
                        ),
                        Map.of("type", "divider"),
                        Map.of(
                                "type", "section",
                                "text", Map.of("type", "mrkdwn", "text", String.format("*Stack Trace (Top %d):*\n```\n    at %s\n```", MAX_STACK_TRACE_LINES, stackTrace))
                        )
                )
        );
    }
}
