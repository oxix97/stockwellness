package org.stockwellness.adapter.batch.stock.step.tasklet;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.batch.stock.config.StockMasterSnapshotProperties;
import org.stockwellness.adapter.batch.stock.snapshot.StockMasterSnapshot;
import org.stockwellness.adapter.batch.stock.snapshot.StockMasterSnapshotStore;
import org.stockwellness.application.port.in.batch.StockMasterSyncUseCase;
import org.stockwellness.domain.stock.MarketType;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockMasterPreflightTasklet implements Tasklet {

    public static final String KOSPI_SNAPSHOT_KEY = "stockMaster.kospi.snapshot";
    public static final String KOSDAQ_SNAPSHOT_KEY = "stockMaster.kosdaq.snapshot";
    public static final String COMPLETENESS_STATUS_KEY = "stockMaster.completenessStatus";
    public static final String STRUCTURALLY_VALID = "STRUCTURALLY_VALID";
    private static final String JOB_NAME = "stockMasterSyncJob";

    private final StockMasterSyncUseCase stockMasterSyncUseCase;
    private final StockMasterSnapshotStore snapshotStore;
    private final JobExplorer jobExplorer;
    private final StockMasterSnapshotProperties properties;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        JobExecution jobExecution = chunkContext.getStepContext().getStepExecution().getJobExecution();
        long executionId = jobExecution.getId();
        var context = jobExecution.getExecutionContext();

        StockMasterSnapshot existingKospi = getSnapshot(context.get(KOSPI_SNAPSHOT_KEY));
        StockMasterSnapshot existingKosdaq = getSnapshot(context.get(KOSDAQ_SNAPSHOT_KEY));
        if (existingKospi != null && existingKosdaq != null) {
            snapshotStore.verifyBoth(existingKospi, existingKosdaq);
            context.putString(COMPLETENESS_STATUS_KEY, STRUCTURALLY_VALID);
            log.info("기존 KIS 종목 마스터 스냅샷을 검증하고 재사용합니다. executionId={}", executionId);
            return RepeatStatus.FINISHED;
        }
        if (existingKospi != null || existingKosdaq != null) {
            context.remove(KOSPI_SNAPSHOT_KEY);
            context.remove(KOSDAQ_SNAPSHOT_KEY);
            context.remove(COMPLETENESS_STATUS_KEY);
            snapshotStore.deleteExecutionArtifacts(executionId);
        }

        cleanupExpiredSnapshots();
        try {
            // Fetch both source artifacts before parsing either one.
            byte[] kospiBytes = stockMasterSyncUseCase.downloadKospiMasterArchive();
            byte[] kosdaqBytes = stockMasterSyncUseCase.downloadKosdaqMasterArchive();
            int kospiCount = stockMasterSyncUseCase.parseKospiMasterArchive(kospiBytes).size();
            int kosdaqCount = stockMasterSyncUseCase.parseKosdaqMasterArchive(kosdaqBytes).size();

            StockMasterSnapshot kospi = snapshotStore.stage(
                    executionId, MarketType.KOSPI, kospiBytes, kospiCount, STRUCTURALLY_VALID);
            StockMasterSnapshot kosdaq = snapshotStore.stage(
                    executionId, MarketType.KOSDAQ, kosdaqBytes, kosdaqCount, STRUCTURALLY_VALID);
            snapshotStore.verifyBoth(kospi, kosdaq);

            context.put(KOSPI_SNAPSHOT_KEY, kospi);
            context.put(KOSDAQ_SNAPSHOT_KEY, kosdaq);
            context.putString(COMPLETENESS_STATUS_KEY, STRUCTURALLY_VALID);
            log.info("KIS 종목 마스터 사전 검증 완료: KOSPI {}건, KOSDAQ {}건, executionId={}",
                    kospiCount, kosdaqCount, executionId);
            return RepeatStatus.FINISHED;
        } catch (RuntimeException exception) {
            context.remove(KOSPI_SNAPSHOT_KEY);
            context.remove(KOSDAQ_SNAPSHOT_KEY);
            context.remove(COMPLETENESS_STATUS_KEY);
            try {
                snapshotStore.deleteExecutionArtifacts(executionId);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new IllegalStateException("KIS 종목 마스터 사전 검증에 실패했습니다", exception);
        }
    }

    private void cleanupExpiredSnapshots() {
        Instant cutoff = Instant.now().minus(properties.retention());
        Set<Long> activeExecutionIds = jobExplorer.findRunningJobExecutions(JOB_NAME).stream()
                .map(JobExecution::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        snapshotStore.cleanupExpired(cutoff, activeExecutionIds);
    }

    private static StockMasterSnapshot getSnapshot(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof StockMasterSnapshot snapshot)) {
            throw new IllegalStateException("Unexpected stock master snapshot descriptor in job context");
        }
        return snapshot;
    }
}
