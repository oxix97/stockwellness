package org.stockwellness.batch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/batch")
@RestController
public class BatchAdminController {
    private final JobLauncher jobLauncher;
    private final Job stockMasterSyncJob;
    private final Job stockPriceBatchJob;

    @PostMapping("/sync-master")
    public String runMasterSync() {
        CompletableFuture.runAsync(() -> {
            try {
                jobLauncher.run(stockMasterSyncJob, new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis()).toJobParameters());
            } catch (Exception e) {
                log.error("Master Sync Job failed", e);
            }
        });
        return "종목 마스터 동기화 시작";
    }

    @PostMapping("/fetch-prices")
    public String runPriceFetch(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate
    ) {
        CompletableFuture.runAsync(() -> {
            try {
                jobLauncher.run(stockPriceBatchJob, new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis())
                        .addString("startDate", startDate)
                        .addString("endDate", endDate)
                        .toJobParameters()
                );
            } catch (Exception e) {
                log.error("Price Fetch Job failed", e);
            }
        });
        return "시세 수집 시작";
    }
}
