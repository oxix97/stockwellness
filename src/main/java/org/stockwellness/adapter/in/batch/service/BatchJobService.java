package org.stockwellness.adapter.in.batch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobService {

    private final JobLauncher jobLauncher;
    private final Job stockHistoryJob;
    private final Job dailyStockJob;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 연간 기록 수집 (동기 실행 권장 - JobLauncher 설정에 따름, 보통 오래 걸리므로 Async 고려 가능하나 일단 유지)
     */
    public Long runYearlyHistory(int year) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("startDate", year + "-01-01")
                .addString("endDate", year + "-12-31")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(stockHistoryJob, jobParameters);
        return execution.getId();
    }

    /**
     * 일일 배치 수동 실행
     */
    public Long runDailyJob(String targetDate) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("requestDate", targetDate)
                .addString("requestType", "MANUAL")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(dailyStockJob, jobParameters);
        return execution.getId();
    }

    /**
     * 전체 재계산 (비동기)
     */
    @Async("batchExecutor")
    public void runTotalBackfillAsync() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("requestType", "TOTAL_BACKFILL")
                    .toJobParameters();

            log.info(">>> Starting Total Backfill (Async)...");
            jobLauncher.run(dailyStockJob, params);
            log.info(">>> Total Backfill Job Finished.");
        } catch (Exception e) {
            log.error("Failed to run total backfill", e);
        }
    }

    /**
     * 기간 백필 (비동기 순차 실행)
     * 기존 컨트롤러의 while 루프 로직을 안전한 Executor 안으로 이동
     */
    @Async("batchExecutor")
    public void runPeriodBackfillAsync(LocalDate start, LocalDate end) {
        log.info("=========== 백필(Backfill) 작업 시작: {} ~ {} ===========", start, end);
        LocalDate current = start;

        while (!current.isAfter(end)) {
            String targetDateStr = current.format(formatter);
            try {
                JobParameters jobParameters = new JobParametersBuilder()
                        .addString("requestDate", targetDateStr)
                        .addString("requestType", "BACKFILL")
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters();

                // 동기적으로 실행 (순차 실행) - DB 부하 조절
                JobExecution execution = jobLauncher.run(dailyStockJob, jobParameters);
                log.info("Job Completed for date: {}, Status: {}", targetDateStr, execution.getStatus());

            } catch (Exception e) {
                log.error("Job Failed for date: {}", targetDateStr, e);
                // 실패해도 다음 날짜 진행 (정책에 따라 중단 가능)
            }
            current = current.plusDays(1);
        }
        log.info("=========== 백필(Backfill) 작업 종료 ===========");
    }
}