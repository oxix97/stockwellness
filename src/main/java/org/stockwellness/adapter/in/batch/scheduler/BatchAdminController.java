package org.stockwellness.adapter.in.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/batch")
@RestController
public class BatchAdminController {
    private final JobLauncher jobLauncher;
    private final Job stockHistoryJob;
    private final Job dailyStockJob;

    @PostMapping
    public ResponseEntity<String> runYearlyHistory(
            @RequestParam("year") int year
    ) {
        try {
            log.info(">>> Request to start Batch for Year: {}", year);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("startDate", year + "-01-01")
                    .addString("endDate", year + "-12-31")
                    // 중복 실행 허용을 위한 유니크 파라미터
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // [4] Job 실행 (start(Job, JobParameters) 사용)
            // 반환값이 Long ID가 아니라 JobExecution 객체입니다.
            JobExecution execution = jobLauncher.run(stockHistoryJob, jobParameters);

            return ResponseEntity.ok(
                    String.format("Batch Started. Year: %d, Execution ID: %d, Status: %s",
                            year, execution.getId(), execution.getStatus())
            );

        } catch (Exception e) {
            log.error("Failed to start batch for year: {}", year, e);
            return ResponseEntity.internalServerError()
                    .body("Failed to start batch: " + e.getMessage());
        }
    }

    @PostMapping("/daily")
    public ResponseEntity<String> runDailyStockJob(
            @RequestParam(required = false) String date
    ) {
        // 날짜가 없으면 오늘 날짜 사용 (yyyyMMdd)
        String targetDate = (date != null) ? date : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        try {
            log.info("수동 배치 요청 감지. Target Date: {}", targetDate);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("requestDate", targetDate)
                    .addString("requestType", "MANUAL") // 실행 타입 구분
                    .addLong("timestamp", System.currentTimeMillis()) // 중복 실행 방지용 유니크 키
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(dailyStockJob, jobParameters);

            return ResponseEntity.ok()
                    .body("Batch Job Started. ID: " + execution.getId() + ", Status: " + execution.getStatus());

        } catch (Exception e) {
            log.error("배치 수동 실행 중 에러 발생", e);
            return ResponseEntity.internalServerError().body("Batch Failed: " + e.getMessage());
        }
    }

    @PostMapping("/all")
    public ResponseEntity<String> runTotalBackfill() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis()) // 유니크 키만 있으면 됨
                    .toJobParameters();

            // 비동기로 실행
            jobLauncher.run(dailyStockJob, params);

            return ResponseEntity.ok("Full Calculation Backfill Started! (Ticker-Based Parallel Processing)");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
