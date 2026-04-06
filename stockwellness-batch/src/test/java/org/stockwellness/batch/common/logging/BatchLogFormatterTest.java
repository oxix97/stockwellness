package org.stockwellness.batch.common.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BatchLogFormatterTest {

    private final BatchLogFormatter formatter = new BatchLogFormatter();

    @Test
    @DisplayName("null 필드는 로그에서 생략한다")
    void omitNullFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("job", "stockPriceBatchJob");
        fields.put("executionId", 12L);
        fields.put("failedItemKey", null);

        String formatted = formatter.format("END", fields);

        assertThat(formatted).isEqualTo("[BATCH][END] job=stockPriceBatchJob executionId=12");
        assertThat(formatted).doesNotContain("failedItemKey");
    }

    @Test
    @DisplayName("진행률과 시간 필드를 운영 로그 형식으로 포맷한다")
    void formatProgressAndDateTime() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 4, 6, 12, 12, 31);

        String formatted = formatter.format("PROGRESS", Map.of(
                "job", "stockPriceBatchJob",
                "progress", 63.739,
                "startedAt", startedAt,
                "elapsed", formatter.formatDuration(3_738_000L)
        ));

        assertThat(formatted).contains("[BATCH][PROGRESS]");
        assertThat(formatted).contains("progress=63.74");
        assertThat(formatted).contains("elapsed=01:02:18");
        assertThat(formatted).contains("startedAt=2026-04-06T12:12:31");
    }
}
