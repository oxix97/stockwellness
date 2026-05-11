package org.stockwellness.batch.support.logging;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class BatchLogFormatter {

    private static final DateTimeFormatter OFFSET_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public String format(String stage, Map<String, Object> fields) {
        String body = fields.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> entry.getKey() + "=" + formatValue(entry.getValue()))
                .collect(Collectors.joining(" "));

        if (body.isEmpty()) {
            return "[BATCH][" + stage + "]";
        }

        return "[BATCH][" + stage + "] " + body;
    }

    public String formatDuration(long durationMs) {
        Duration duration = Duration.ofMillis(Math.max(durationMs, 0L));
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).format(OFFSET_DATE_TIME_FORMATTER);
    }

    public Map<String, Object> orderedFields() {
        return new LinkedHashMap<>();
    }

    private String formatValue(Object value) {
        if (value instanceof Double doubleValue) {
            return BigDecimal.valueOf(doubleValue)
                    .setScale(2, RoundingMode.HALF_UP)
                    .toPlainString();
        }
        if (value instanceof Float floatValue) {
            return BigDecimal.valueOf(floatValue)
                    .setScale(2, RoundingMode.HALF_UP)
                    .toPlainString();
        }
        if (value instanceof LocalDateTime dateTime) {
            return formatDateTime(dateTime);
        }
        return String.valueOf(value);
    }
}
