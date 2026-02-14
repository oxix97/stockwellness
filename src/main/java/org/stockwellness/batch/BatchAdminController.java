package org.stockwellness.adapter.in.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.stockwellness.adapter.in.batch.service.BatchJobService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/batch")
@RestController
public class BatchAdminController {
    private final JobLauncher jobLauncher;
    private final Job stockMasterSyncJob;
    private final Job stockPriceBatchJob;
    private final BatchJobService batchJobService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    @PostMapping
    public ResponseEntity<String> runYearlyHistory(@RequestParam("year") int year) {
        try {
            Long executionId = batchJobService.runYearlyHistory(year);
            return ResponseEntity.ok("Batch Started. Execution ID: " + executionId);
        } catch (Exception e) {
            log.error("Failed to start batch for year: {}", year, e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/daily")
    public ResponseEntity<String> runDailyStockJob(@RequestParam(required = false) String date) {
        String targetDate = (date != null) ? date : LocalDate.now().format(formatter);
        try {
            Long executionId = batchJobService.runDailyJob(targetDate);
            return ResponseEntity.ok("Daily Batch Started. Execution ID: " + executionId);
        } catch (Exception e) {
            log.error("Failed to start daily batch", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/all")
    public ResponseEntity<String> runTotalBackfill() {
        batchJobService.runTotalBackfillAsync();
        return ResponseEntity.ok("Full Calculation Backfill Request Accepted (Running in Background).");
    }

    @PostMapping("/backfill")
    public ResponseEntity<String> runBackfillStockJob(
            @RequestParam(required = false, defaultValue = "20210104") String startDate,
            @RequestParam(required = false) String endDate
    ) {
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = (endDate != null) ? LocalDate.parse(endDate, formatter) : LocalDate.now();

        if (start.isAfter(end)) {
            return ResponseEntity.badRequest().body("Start date cannot be after end date.");
        }

        batchJobService.runPeriodBackfillAsync(start, end);

        return ResponseEntity.ok(
                String.format("Backfill Request Accepted. Range: %s ~ %s (Running in Background)", start, end)
        );
    }

    @PostMapping("/sync-master")
    public String runMasterSync() throws Exception {
        jobLauncher.run(stockMasterSyncJob, new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis()).toJobParameters());
        return "종목 마스터 동기화 시작";
    }

    @PostMapping("/fetch-prices")
    public String runPriceFetch() throws Exception {
        jobLauncher.run(stockPriceBatchJob, new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis()).toJobParameters());
        return "5년치 시세 수집 시작";
    }
}
