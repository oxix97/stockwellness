package org.stockwellness.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.stockwellness.application.service.portfolio.AdvisorOrchestrator;
import org.stockwellness.batch.exception.BatchException;
import org.stockwellness.global.error.ErrorCode;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockwellnessScheduler {

    private final JobLauncher jobLauncher;
    private final AdvisorOrchestrator advisorOrchestrator;
    
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
        log.info(">>> 일일 전체 데이터 동기화 배치 시작 [시작 시각: {}]", timestamp);

        try {
            // 1. 종목 마스터 동기화
            log.info("[1단계] 종목 마스터 데이터 동기화 시작...");
            JobParameters masterParams = new JobParametersBuilder()
                    .addLong("time", timestamp)
                    .toJobParameters();
            JobExecution masterExecution = jobLauncher.run(stockMasterSyncJob, masterParams);
            validateStatus(masterExecution);

            // 2. 가격 정보 동기화 (Kafka 이벤트 발행 활성화)
            log.info("[2단계] 종목 시세 동기화 및 카프카 이벤트 발행 시작...");
            JobParameters priceParams = new JobParametersBuilder()
                    .addLong("time", timestamp)
                    .addString("publishEvent", "true")
                    .toJobParameters();
            JobExecution priceExecution = jobLauncher.run(stockPriceBatchJob, priceParams);
            validateStatus(priceExecution);

            // 3. 섹터 인사이트 동기화
            log.info("[3단계] 섹터 인사이트 및 AI 분석 동기화 시작...");
            JobParameters sectorParams = new JobParametersBuilder()
                    .addLong("time", timestamp)
                    .toJobParameters();
            JobExecution sectorExecution = jobLauncher.run(sectorEodJob, sectorParams);
            validateStatus(sectorExecution);

            log.info(">>> 일일 전체 데이터 동기화 배치 성공적으로 완료.");
        } catch (Exception e) {
            log.error(">>> 일일 전체 동기화 배치 중 심각한 오류 발생: {}", e.getMessage(), e);
            throw new BatchException(ErrorCode.BATCH_ORCHESTRATION_FAILED);
        }
    }

    /**
     * 매주 월요일 오전 8시에 모든 포트폴리오에 대한 AI 리밸런싱 조언을 생성합니다.
     */
    @Scheduled(cron = "0 0 8 * * MON")
    public void runAiAdvisorRebalancing() {
        log.info(">>> 주간 AI 어드바이저 리밸런싱 오케스트레이션 시작...");
        try {
            advisorOrchestrator.runAllPortfolios();
            log.info(">>> 주간 AI 어드바이저 리밸런싱 완료.");
        } catch (Exception e) {
            log.error(">>> AI 어드바이저 리밸런싱 실행 실패: {}", e.getMessage(), e);
        }
    }

    private void validateStatus(JobExecution jobExecution) {
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            throw new BatchException(ErrorCode.BATCH_ORCHESTRATION_FAILED);
        }
    }
}
