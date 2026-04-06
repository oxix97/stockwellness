package org.stockwellness.batch.listener;

import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 쓰기 에러 발생 시 실패한 아이템의 ID를 수집하여 StepExecutionContext에 저장하는 리스너
 */
public class BatchFailureItemListener<T> implements ItemWriteListener<T> {

    public static final String FAILED_ITEM_IDS = "FAILED_ITEM_IDS";
    public static final String LAST_FAILED_ITEM_ID = "LAST_FAILED_ITEM_ID";
    public static final String LAST_FAILED_ITEM_KEY = "LAST_FAILED_ITEM_KEY";

    private final Function<T, List<String>> idExtractor;
    private final Function<T, String> keyExtractor;
    private StepExecution stepExecution;

    public BatchFailureItemListener(Function<T, List<String>> idExtractor) {
        this(idExtractor, item -> null);
    }

    public BatchFailureItemListener(Function<T, List<String>> idExtractor, Function<T, String> keyExtractor) {
        this.idExtractor = idExtractor;
        this.keyExtractor = keyExtractor;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public void onWriteError(Exception exception, Chunk<? extends T> items) {
        if (stepExecution == null) return;

        List<String> failedIds = new ArrayList<>();
        for (T item : items) {
            failedIds.addAll(idExtractor.apply(item));
        }

        @SuppressWarnings("unchecked")
        List<String> currentFailedIds = (List<String>) stepExecution.getExecutionContext().get(FAILED_ITEM_IDS);
        if (currentFailedIds == null) {
            currentFailedIds = new ArrayList<>();
        } else {
            // Immutable 리스트일 수 있으므로 새로운 리스트로 복사
            currentFailedIds = new ArrayList<>(currentFailedIds);
        }
        
        currentFailedIds.addAll(failedIds);
        stepExecution.getExecutionContext().put(FAILED_ITEM_IDS, currentFailedIds);

        // 종료 로그와 lifecycle 이벤트에서 마지막 실패 대상을 바로 참조할 수 있게 별도 키로 남긴다.
        String lastFailedId = failedIds.stream()
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(null);
        String lastFailedKey = items.getItems().stream()
                .map(keyExtractor)
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(null);

        stepExecution.getExecutionContext().put(LAST_FAILED_ITEM_ID, lastFailedId);
        stepExecution.getExecutionContext().put(LAST_FAILED_ITEM_KEY, lastFailedKey);
    }
}
