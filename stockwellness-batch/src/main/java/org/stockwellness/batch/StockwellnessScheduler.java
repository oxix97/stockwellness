package org.stockwellness.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.stockwellness.batch.exception.BatchException;
import org.stockwellness.global.error.ErrorCode;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockwellnessScheduler {

    private final JobLauncher jobLauncher;
    
    // 주입할 배치 잡들
    private final Job stockMasterSyncJob;
    private final Job stockPriceBatchJob;
    private final Job sectorEodJob;

    /**
     * 매일 평일(월-금) 오후 3시 30분에 전체 데이터 동기화 배치를 실행합니다.
     * 순서: 종목 마스터 -> 시세 정보(Kafka 이벤트 발행) -> 섹터 인사이트
     */
    @Scheduled(cron = "0 30 15 * * MON-FRI")
    public void runDailyFullSync() {
        long timestamp = System.currentTimeMillis();
        log.info(">>> Starting Daily Full Sync Batch [Time: {}]", timestamp);

        try {
            // 1. 종목 마스터 동기화
            log.info("Step 1: Running Stock Master Sync...");
            JobParameters masterParams = new JobParametersBuilder()
                    .addLong("time", timestamp)
                    .toJobParameters();
            JobExecution masterExecution = jobLauncher.run(stockMasterSyncJob, masterParams);
            validateStatus(masterExecution);

            // 2. 가격 정보 동기화 (Kafka 이벤트 발행 활성화)
            log.info("Step 2: Running Stock Price Sync with Kafka Event Publishing...");
            JobParameters priceParams = new JobParametersBuilder()
                    .addLong("time", timestamp)
                    .addString("publishEvent", "true")
                    .toJobParameters();
            JobExecution priceExecution = jobLauncher.run(stockPriceBatchJob, priceParams);
            validateStatus(priceExecution);

            // 3. 섹터 인사이트 동기화
            log.info("Step 3: Running Sector Insight Sync...");
            JobParameters sectorParams = new JobParametersBuilder()
                    .addLong("time", timestamp)
                    .toJobParameters();
            JobExecution sectorExecution = jobLauncher.run(sectorEodJob, sectorParams);
            validateStatus(sectorExecution);

            log.info(">>> Daily Full Sync Batch Completed Successfully.");
        } catch (Exception e) {
            log.error(">>> Critical error occurred during Daily Full Sync Batch: {}", e.getMessage(), e);
            throw new BatchException(ErrorCode.BATCH_ORCHESTRATION_FAILED);
        }
    }

    private void validateStatus(JobExecution jobExecution) {
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            throw new BatchException(ErrorCode.BATCH_ORCHESTRATION_FAILED);
        }
    }
}
