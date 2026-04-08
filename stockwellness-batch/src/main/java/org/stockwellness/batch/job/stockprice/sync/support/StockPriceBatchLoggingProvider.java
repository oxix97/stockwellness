package org.stockwellness.batch.job.stockprice.sync.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;
import org.stockwellness.batch.support.logging.BatchFailureSummary;
import org.stockwellness.batch.support.logging.BatchFailureSummaryProvider;
import org.stockwellness.batch.support.logging.BatchLoggingConstants;
import org.stockwellness.batch.support.logging.BatchProgressSnapshot;
import org.stockwellness.batch.support.logging.BatchProgressSnapshotProvider;
import org.stockwellness.batch.support.logging.BatchStartSummary;
import org.stockwellness.batch.support.logging.BatchStartSummaryProvider;
import org.stockwellness.batch.support.listener.BatchFailureItemListener;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.global.util.DateUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class StockPriceBatchLoggingProvider implements
        BatchStartSummaryProvider,
        BatchProgressSnapshotProvider,
        BatchFailureSummaryProvider {

    private static final String JOB_NAME = "stockPriceBatchJob";

    private final EntityManagerFactory entityManagerFactory;

    @Override
    public String jobName() {
        return JOB_NAME;
    }

    @Override
    public synchronized BatchStartSummary initialize(JobExecution jobExecution) {
        String startDate = jobExecution.getExecutionContext().containsKey(BatchLoggingConstants.CTX_START_DATE)
                ? (String) jobExecution.getExecutionContext().get(BatchLoggingConstants.CTX_START_DATE)
                : jobExecution.getJobParameters().getString("startDate");
        String endDate = jobExecution.getExecutionContext().containsKey(BatchLoggingConstants.CTX_END_DATE)
                ? (String) jobExecution.getExecutionContext().get(BatchLoggingConstants.CTX_END_DATE)
                : resolveEndDate(jobExecution);

        if (!jobExecution.getExecutionContext().containsKey(BatchLoggingConstants.CTX_TOTAL_COUNT)) {
            // START 로그와 STARTED Kafka 이벤트가 모두 같은 시작 정보를 재사용할 수 있게 최초 1회만 계산한다.
            long totalCount = countTargetStocks(jobExecution.getJobParameters().getString("targetTicker"));
            jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_START_DATE, startDate);
            jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_END_DATE, endDate);
            jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_TOTAL_COUNT, totalCount);
            jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_PROCESSED_COUNT, 0L);
            jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_LAST_LOGGED_PROCESSED_COUNT, 0L);
        }

        Long totalCount = getLong(jobExecution, BatchLoggingConstants.CTX_TOTAL_COUNT);
        return new BatchStartSummary(startDate, endDate, totalCount);
    }

    @Override
    public synchronized void updateProgress(StepExecution stepExecution, Chunk<?> items) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        long processedCount = getLong(jobExecution, BatchLoggingConstants.CTX_PROCESSED_COUNT) == null
                ? 0L
                : getLong(jobExecution, BatchLoggingConstants.CTX_PROCESSED_COUNT);

        long increment = 0L;
        Long currentItemId = null;
        String currentItemKey = null;

        for (Object item : items) {
            if (!(item instanceof List<?> list)) {
                continue;
            }
            // writer 입력은 "종목 1개당 여러 StockPrice"가 아니라 "최대 5종목 묶음의 StockPrice 목록"이므로
            // row 수가 아닌 distinct stock id 수로 처리량을 계산한다.
            increment += list.stream()
                    .filter(StockPrice.class::isInstance)
                    .map(StockPrice.class::cast)
                    .map(stockPrice -> stockPrice.getId().getStockId())
                    .distinct()
                    .count();

            StockPrice lastStockPrice = list.stream()
                    .filter(StockPrice.class::isInstance)
                    .map(StockPrice.class::cast)
                    .filter(Objects::nonNull)
                    .reduce((first, second) -> second)
                    .orElse(null);

            if (lastStockPrice != null) {
                currentItemId = lastStockPrice.getId().getStockId();
                currentItemKey = lastStockPrice.getStock() == null ? null : lastStockPrice.getStock().getTicker();
            }
        }

        long updatedProcessedCount = processedCount + increment;
        jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_PROCESSED_COUNT, updatedProcessedCount);
        jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_CURRENT_ITEM_ID, currentItemId);
        jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_CURRENT_ITEM_KEY, currentItemKey);
        jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_LAST_UPDATED_AT, LocalDateTime.now());

        Long totalCount = getLong(jobExecution, BatchLoggingConstants.CTX_TOTAL_COUNT);
        if (totalCount != null && updatedProcessedCount > 0 && updatedProcessedCount < totalCount && jobExecution.getStartTime() != null) {
            long elapsedMs = Duration.between(jobExecution.getStartTime(), LocalDateTime.now()).toMillis();
            long remaining = totalCount - updatedProcessedCount;
            // 단순 평균 속도 기반 ETA: elapsed / processed * remaining
            long estimatedRemainingMs = Math.max((elapsedMs / updatedProcessedCount) * remaining, 0L);
            jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_ESTIMATED_REMAINING_MS, estimatedRemainingMs);
            return;
        }

        jobExecution.getExecutionContext().put(BatchLoggingConstants.CTX_ESTIMATED_REMAINING_MS, null);
    }

    @Override
    public synchronized BatchProgressSnapshot getProgressSnapshot(JobExecution jobExecution) {
        Long totalCount = getLong(jobExecution, BatchLoggingConstants.CTX_TOTAL_COUNT);
        Long processedCount = getLong(jobExecution, BatchLoggingConstants.CTX_PROCESSED_COUNT);
        Long currentItemId = getLong(jobExecution, BatchLoggingConstants.CTX_CURRENT_ITEM_ID);
        String currentItemKey = getString(jobExecution, BatchLoggingConstants.CTX_CURRENT_ITEM_KEY);
        Long estimatedRemainingMs = getLong(jobExecution, BatchLoggingConstants.CTX_ESTIMATED_REMAINING_MS);

        Double progressPercent = null;
        if (totalCount != null && totalCount > 0 && processedCount != null) {
            progressPercent = (processedCount.doubleValue() * 100.0d) / totalCount.doubleValue();
        }

        return new BatchProgressSnapshot(
                processedCount == null ? 0L : processedCount,
                totalCount,
                progressPercent,
                currentItemId,
                currentItemKey,
                "currentStockId",
                "ticker",
                estimatedRemainingMs
        );
    }

    @Override
    public synchronized BatchFailureSummary getFailureSummary(JobExecution jobExecution) {
        Long failedItemId = getLong(jobExecution, BatchLoggingConstants.CTX_FAILED_ITEM_ID);
        String failedItemKey = getString(jobExecution, BatchLoggingConstants.CTX_FAILED_ITEM_KEY);

        if (failedItemId != null || failedItemKey != null) {
            return new BatchFailureSummary(failedItemId, failedItemKey);
        }

        return jobExecution.getStepExecutions().stream()
                .map(stepExecution -> {
                    Object idValue = stepExecution.getExecutionContext().get(BatchFailureItemListener.LAST_FAILED_ITEM_ID);
                    Object keyValue = stepExecution.getExecutionContext().get(BatchFailureItemListener.LAST_FAILED_ITEM_KEY);
                    Long parsedId = idValue == null ? null : parseLong(String.valueOf(idValue));
                    String parsedKey = keyValue == null ? null : String.valueOf(keyValue);
                    if (parsedId == null && parsedKey == null) {
                        return null;
                    }
                    return new BatchFailureSummary(parsedId, parsedKey);
                })
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(new BatchFailureSummary(null, null));
    }

    private long countTargetStocks(String targetTicker) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            String query = StockPriceBatchTargetQuery.countQuery(targetTicker);
            Map<String, Object> parameters = StockPriceBatchTargetQuery.parameters(targetTicker);
            var typedQuery = entityManager.createQuery(query, Long.class);
            parameters.forEach(typedQuery::setParameter);
            return typedQuery.getSingleResult();
        } finally {
            entityManager.close();
        }
    }

    private String resolveEndDate(JobExecution jobExecution) {
        String endDate = jobExecution.getJobParameters().getString("endDate");
        if (endDate != null) {
            return endDate;
        }
        return DateUtil.today().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    }

    private Long getLong(JobExecution jobExecution, String key) {
        Object value = jobExecution.getExecutionContext().get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? null : parseLong(String.valueOf(value));
    }

    private String getString(JobExecution jobExecution, String key) {
        Object value = jobExecution.getExecutionContext().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
