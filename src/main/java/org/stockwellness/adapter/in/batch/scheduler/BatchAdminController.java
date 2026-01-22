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
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/batch")
@RestController
public class BatchAdminController {
    private final JobLauncher jobLauncher;
    private final Job stockHistoryJob;
    private final Job dailyStockJob;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

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

    @PostMapping("/backfill")
    public ResponseEntity<String> runBackfillStockJob(
            @RequestParam(required = false, defaultValue = "20210104") String startDate,
            @RequestParam(required = false) String endDate
    ) {
        // 1. 날짜 파싱 (String -> LocalDate)
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = (endDate != null)
                ? LocalDate.parse(endDate, formatter)
                : LocalDate.now(); // endDate가 없으면 오늘까지

        if (start.isAfter(end)) {
            return ResponseEntity.badRequest().body("시작 날짜가 종료 날짜보다 뒤에 있습니다.");
        }

        // 2. 비동기 백그라운드 실행 (클라이언트는 즉시 응답을 받음)
        CompletableFuture.runAsync(() -> {
            LocalDate current = start;

            log.info("=========== 백필(Backfill) 작업 시작: {} ~ {} ===========", start, end);

            while (!current.isAfter(end)) {
                String targetDateStr = current.format(formatter);

                try {
                    JobParameters jobParameters = new JobParametersBuilder()
                            .addString("requestDate", targetDateStr)
                            .addString("requestType", "BACKFILL") // 구분값 변경
                            .addLong("timestamp", System.currentTimeMillis()) // 중복 실행 허용
                            .toJobParameters();

                    // 동기적으로 실행 (순차 실행) - DB 부하 조절을 위해
                    JobExecution execution = jobLauncher.run(dailyStockJob, jobParameters);

                    log.info("Job Completed for date: {}, Status: {}", targetDateStr, execution.getStatus());

                } catch (Exception e) {
                    // 하루치가 실패해도 다음 날짜는 계속 진행할지, 멈출지 결정 (여기선 로그 찍고 계속 진행)
                    log.error("Job Failed for date: {}", targetDateStr, e);
                }

                // 다음 날짜로 이동
                current = current.plusDays(1);
            }

            log.info("=========== 백필(Backfill) 작업 종료 ===========");
        });

        // 3. 즉시 응답 반환
        return ResponseEntity.ok()
                .body(String.format("백필 작업이 백그라운드에서 시작되었습니다. 기간: %s ~ %s", start, end));
    }
}
