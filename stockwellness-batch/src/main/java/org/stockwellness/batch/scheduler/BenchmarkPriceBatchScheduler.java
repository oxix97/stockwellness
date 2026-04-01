package org.stockwellness.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BenchmarkPriceBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job benchmarkPriceSyncJob;

    /**
     * 매일 오전 8시(KST) 지수 시세 동기화 배치 실행
     * 미국 시장 마감(오전 6~7시) 후 수집하기 적절한 시간
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void runBenchmarkPriceSyncJob() {
        log.info("[Scheduler] 지수 시세 동기화 배치 자동 실행 시작");

        // 어제 날짜부터 수집
        String startDate = LocalDate.now().minusDays(1).toString();

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("startDate", startDate)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(benchmarkPriceSyncJob, params);
            log.info("[Scheduler] 지수 시세 동기화 배치 트리거 성공");

        } catch (Exception e) {
            log.error("[Scheduler] 배치 실행 중 오류 발생: {}", e.getMessage());
        }
    }
}
