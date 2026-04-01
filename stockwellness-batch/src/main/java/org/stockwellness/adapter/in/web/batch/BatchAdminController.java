package org.stockwellness.adapter.in.web.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/batch")
@RequiredArgsConstructor
public class BatchAdminController {

    private final JobLauncher jobLauncher;
    private final Job benchmarkPriceSyncJob;

    /**
     * 지수 시세 동기화 배치 수동 실행
     * @param startDate 수집 시작일 (yyyy-MM-dd, 선택 사항)
     */
    @PostMapping("/benchmark-sync")
    public ResponseEntity<String> syncBenchmarkPrice(@RequestParam(required = false) String startDate) {
        log.info("[Batch Admin] 지수 시세 동기화 수동 실행 요청 (startDate: {})", startDate);

        // 날짜 형식 유효성 검증
        if (startDate != null) {
            try {
                LocalDate.parse(startDate);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().body("Invalid startDate format. Use yyyy-MM-dd");
            }
        }

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("startDate", startDate)
                    .addLong("timestamp", System.currentTimeMillis()) // 중복 실행 방지 및 매번 새로운 실행 보장
                    .toJobParameters();

            jobLauncher.run(benchmarkPriceSyncJob, params);
            return ResponseEntity.ok("Benchmark price sync job started successfully");

        } catch (Exception e) {
            log.error("[Batch Admin] 배치 실행 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to start batch job: " + e.getMessage());
        }
    }
}
