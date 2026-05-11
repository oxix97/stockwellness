package org.stockwellness.global.alert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SlackMessageBuilder {
    private final Environment env;

    public Map<String, Object> build(NotificationContext context) {
        String activeProfile = getActiveProfile();
        String color = context.type() == NotificationContext.NotificationType.ERROR ? "#E01E5A" : "#2EB67D";
        
        List<Map<String, Object>> fields = new ArrayList<>();
        if (context.traceId() != null) fields.add(field("Trace ID", "`" + context.traceId() + "`"));
        if (context.time() != null) fields.add(field("Time", context.time()));
        if (context.userId() != null) fields.add(field("User ID", context.userId()));
        if (context.url() != null) fields.add(field("URL", context.url()));
        
        if (context.details() != null) {
            context.details().forEach((key, value) -> fields.add(field(key, value)));
        }

        List<Map<String, Object>> blocks = new ArrayList<>();
        blocks.add(header(String.format("%s [%s] %s", getEmoji(context.type()), activeProfile, context.title())));
        
        if (!fields.isEmpty()) {
            addFieldsInSections(blocks, fields);
        }
        
        if (context.exceptionType() != null) {
            blocks.add(sectionMrkdwn("*오류 요약:*\n*`" + context.exceptionType() + "`*\n> " + context.content()));
        } else {
            blocks.add(sectionMrkdwn(context.content()));
        }
        
        blocks.add(divider());
        
        if (context.stackTrace() != null) {
            blocks.add(sectionMrkdwn("*Stack Trace (Top 5):*\n```\n" + context.stackTrace() + "\n```"));
        } else {
            blocks.add(sectionMrkdwn("_상세 내용은 로그를 확인하세요_"));
        }

        return Map.of(
            "attachments", List.of(
                Map.of(
                    "color", color,
                    "blocks", blocks
                )
            )
        );
    }

    private String getActiveProfile() {
        String[] profiles = env.getActiveProfiles();
        if (profiles.length == 0) {
            return "LOCAL";
        }
        return String.join(", ", profiles).toUpperCase();
    }

    private void addFieldsInSections(List<Map<String, Object>> blocks, List<Map<String, Object>> fields) {
        final int MAX_FIELDS = 10;
        for (int i = 0; i < fields.size(); i += MAX_FIELDS) {
            int end = Math.min(i + MAX_FIELDS, fields.size());
            blocks.add(sectionFields(fields.subList(i, end)));
        }
    }

    private Map<String, Object> header(String text) {
        return Map.of("type", "header", "text", Map.of("type", "plain_text", "text", text));
    }
    
    private Map<String, Object> sectionFields(List<Map<String, Object>> fields) {
        return Map.of("type", "section", "fields", fields);
    }
    
    private Map<String, Object> sectionMrkdwn(String text) {
        return Map.of("type", "section", "text", Map.of("type", "mrkdwn", "text", text));
    }

    private Map<String, Object> field(String label, String value) {
        return Map.of("type", "mrkdwn", "text", "*" + label + ":*\n" + value);
    }

    private Map<String, Object> divider() { return Map.of("type", "divider"); }

    private String getEmoji(NotificationContext.NotificationType type) {
        return switch (type) {
            case ERROR -> "🚨";
            case SUCCESS -> "✅";
            default -> "ℹ️";
        };
    }
}
