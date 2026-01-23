package org.stockwellness.adapter.in.batch.scheduler;


import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class StockScheduler {
    private final JobLauncher jobLauncher;
    private final Job dailyStockJob;

    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Seoul")
    public void runKrxDailyBatch() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("requestDate", today)
                    .addString("requestType", "SCHEDULED")
                    .addLong("timestamp", System.currentTimeMillis()) // 실행 시점 유니크 ID
                    .toJobParameters();

            jobLauncher.run(dailyStockJob, jobParameters);

        } catch (Exception e) {
            e.printStackTrace();
            // 추후 여기에 Slack/Email 알림 로직 추가 (Alerting)
        }
    }
}
