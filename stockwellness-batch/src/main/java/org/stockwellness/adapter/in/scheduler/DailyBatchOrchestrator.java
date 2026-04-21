package org.stockwellness.adapter.in.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyBatchOrchestrator {

    private final JobLauncher jobLauncher;
    private final Job stockMasterSyncJob;
    private final Job dailyStockPriceBatchJob;
    private final Job stockInvestorTradeDetailJob;
    private final Job sectorEodJob;
    private final Job benchmarkPriceSyncJob;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 1. 주식 종목 동기화 <br>
     * 2. 기관, 외인 거래 내역 동기화 <br>
     * 3. 주식 가격 정보 및 기술 지표 동기화 <br>
     */
    public void runDailyStockSync() {
        runDailyStockSync(org.stockwellness.global.util.DateUtil.today());
    }

    public void runDailyStockSync(LocalDate targetDate) {
        try {
            JobExecution stockSync = executeJob(stockMasterSyncJob, targetDate);
            if (isFailed(stockSync)) return;

            JobExecution stockPriceSync = executeJob(dailyStockPriceBatchJob, targetDate);
            if (isFailed(stockPriceSync)) return;

            JobExecution stockInvestorTradeDetailSync = executeJob(stockInvestorTradeDetailJob, targetDate);
            if (isFailed(stockInvestorTradeDetailSync)) return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 1. 업종 지수 동기화 <br>
     * 2. AI 인사이트 작성 (미구현)
     */
    public void runDailySectorInsightSync() {
        try {
            JobExecution sectorSync = executeJob(sectorEodJob);
            if (isFailed(sectorSync)) return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 벤치마크 정보 동기화: KOSPI 200, S&P500 등..
     */
    public void runDailyMarketSync() {
        try {
            JobExecution benchmarkSync = executeJob(benchmarkPriceSyncJob);
            if (isFailed(benchmarkSync)) return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JobExecution executeJob(Job job) throws Exception {
        return executeJob(job, org.stockwellness.global.util.DateUtil.today());
    }

    private JobExecution executeJob(Job job, LocalDate targetDate) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLocalDateTime("runTime", LocalDateTime.now()) // 매번 새로운 JobInstance 생성을 위함
                .addString("publishEvent", "true")
                .addString("startDate", targetDate.minusDays(7).format(formatter))
                .addString("endDate", targetDate.format(formatter))
                .addString("targetDate", targetDate.format(formatter))
                .toJobParameters();

        return jobLauncher.run(job, params);
    }

    private boolean isFailed(JobExecution execution) {
        if (execution.getStatus().isUnsuccessful()) {
            log.error("Job {} failed. Stopping workflow. Status: {}",
                    execution.getJobInstance().getJobName(), execution.getStatus());
            return true;
        }
        return false;
    }
}
