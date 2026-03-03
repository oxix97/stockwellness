package org.stockwellness.batch.job.stock.master;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.stockwellness.global.util.DateUtil;

import java.time.LocalDateTime;

/**
 * 종목 마스터 동기화 Job 스케줄러.
 * 매일 오전 7시(KST) {@code stockMasterSyncJob}을 실행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockMasterSyncScheduler {

    private final JobLauncher jobLauncher;
    private final Job stockMasterSyncJob;

    @Scheduled(cron = "0 0 16 * * *", zone = "Asia/Seoul")
    public void schedule() {
        try {
            var params = new JobParametersBuilder()
                    .addLocalDateTime("startedAt", DateUtil.now())
                    .toJobParameters();

            log.info("[StockMasterSyncScheduler] Job 기동");
            jobLauncher.run(stockMasterSyncJob, params);

        } catch (Exception e) {
            log.error("[StockMasterSyncScheduler] Job 기동 실패", e);
        }
    }
}
