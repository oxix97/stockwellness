package org.stockwellness.batch.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.test.MetaDataInstanceFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BatchFailureItemListenerTest {

    private BatchFailureItemListener<List<String>> listener;
    private StepExecution stepExecution;

    @BeforeEach
    void setUp() {
        // 입력이 List<String>이고, 각 요소 자체가 ID 리스트인 경우의 추출기
        listener = new BatchFailureItemListener<>(items -> items);
        stepExecution = MetaDataInstanceFactory.createStepExecution();
        listener.beforeStep(stepExecution);
    }

    @Test
    @DisplayName("쓰기 에러 발생 시 실패한 ID들을 ExecutionContext에 저장한다")
    @SuppressWarnings("unchecked")
    void onWriteErrorSavesIds() {
        // given
        List<String> failedItems = List.of("ID1", "ID2");
        Chunk<List<String>> chunk = new Chunk<>(List.of(failedItems));

        // when
        listener.onWriteError(new Exception("test error"), chunk);

        // then
        List<String> storedIds = (List<String>) stepExecution.getExecutionContext().get(BatchFailureItemListener.FAILED_ITEM_IDS);
        assertThat(storedIds).containsExactlyInAnyOrder("ID1", "ID2");
    }

    @Test
    @DisplayName("여러 번 에러 발생 시 실패한 ID들이 누적된다")
    @SuppressWarnings("unchecked")
    void onWriteErrorAccumulatesIds() {
        // given
        listener.onWriteError(new Exception("error 1"), new Chunk<>(List.of(List.of("ID1"))));

        // when
        listener.onWriteError(new Exception("error 2"), new Chunk<>(List.of(List.of("ID2"))));

        // then
        List<String> storedIds = (List<String>) stepExecution.getExecutionContext().get(BatchFailureItemListener.FAILED_ITEM_IDS);
        assertThat(storedIds).containsExactlyInAnyOrder("ID1", "ID2");
    }
}
