package org.stockwellness.domain.shared.event;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BatchResultEventTest {

    @Test
    @DisplayName("성공 이벤트 생성 시 필드가 올바르게 설정된다")
    void successEvent() {
        // given
        String batchName = "price-batch";
        long processedCount = 100L;
        long executionTime = 500L;

        // when
        BatchResultEvent event = BatchResultEvent.success(batchName, processedCount, executionTime);

        // then
        assertThat(event.batchName()).isEqualTo(batchName);
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.processedCount()).isEqualTo(processedCount);
        assertThat(event.successCount()).isEqualTo(processedCount);
        assertThat(event.failedCount()).isZero();
        assertThat(event.failedIdList()).isEmpty();
        assertThat(event.executionTime()).isEqualTo(executionTime);
        assertThat(event.errorMessage()).isNull();
    }

    @Test
    @DisplayName("실패 이벤트 생성 시 필드가 올바르게 설정된다")
    void failureEvent() {
        // given
        String batchName = "indicator-batch";
        long processedCount = 100L;
        long successCount = 98L;
        List<String> failedIdList = List.of("STK001", "STK002");
        long executionTime = 600L;
        String errorMessage = "Connection timeout";

        // when
        BatchResultEvent event = BatchResultEvent.failure(batchName, processedCount, successCount, failedIdList, executionTime, errorMessage);

        // then
        assertThat(event.batchName()).isEqualTo(batchName);
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.processedCount()).isEqualTo(processedCount);
        assertThat(event.successCount()).isEqualTo(successCount);
        assertThat(event.failedCount()).isEqualTo(2);
        assertThat(event.failedIdList()).containsExactly("STK001", "STK002");
        assertThat(event.executionTime()).isEqualTo(executionTime);
        assertThat(event.errorMessage()).isEqualTo(errorMessage);
    }
}
