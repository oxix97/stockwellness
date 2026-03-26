package org.stockwellness.global.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

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
            String message = buildMessage(traceId, e);
            restClient.post()
                    .uri(URI.create(properties.webhookUrl()))
                    .body(Map.of("text", message))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[SlackAlert] 알림 전송 성공: traceId={}", traceId);
        } catch (Exception ex) {
            log.error("[SlackAlert] 알림 전송 실패: traceId={}, reason={}", traceId, ex.getMessage());
        }
    }

    private String buildMessage(String traceId, Exception e) {
        String stackTrace = Arrays.stream(e.getStackTrace())
                .limit(MAX_STACK_TRACE_LINES)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n    at "));

        String time = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).format(FORMATTER);

        return String.format(
                """
                🚨 *[500 INTERNAL_SERVER_ERROR]*
                • *traceId*: %s
                • *exception*: %s - %s
                • *stackTrace*:
                    at %s
                • *time*: %s
                """,
                traceId,
                e.getClass().getSimpleName(),
                e.getMessage(),
                stackTrace,
                time
        );
    }
}
