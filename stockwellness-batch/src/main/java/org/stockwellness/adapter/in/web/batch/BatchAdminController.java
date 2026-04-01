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

import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.batch.exception.BatchException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.JobParametersInvalidException;
import java.util.concurrent.CompletableFuture;

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
     * @param endDate 수집 종료일 (yyyy-MM-dd, 선택 사항)
     */
    @PostMapping("/benchmark-sync")
    public ResponseEntity<String> syncBenchmarkPrice(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("[Batch Admin] 지수 시세 동기화 수동 실행 요청 수신 (시작일: {}, 종료일: {})", startDate, endDate);

        // 날짜 형식 유효성 검증
        validateDateFormat(startDate, "시작일");
        validateDateFormat(endDate, "종료일");

        final JobParameters params = new JobParametersBuilder()
                .addString("startDate", startDate)
                .addString("endDate", endDate)
                .addLong("timestamp", System.currentTimeMillis()) // 매 실행마다 고유한 파라미터 부여
                .toJobParameters();

        // 긴 실행 시간으로 인한 타임아웃 방지를 위해 비동기(Async) 실행
        CompletableFuture.runAsync(() -> {
            try {
                log.info("[Batch Admin] 지수 시세 동기화 배치 비동기 실행 시작");
                jobLauncher.run(benchmarkPriceSyncJob, params);
                log.info("[Batch Admin] 지수 시세 동기화 배치 비동기 실행 완료");
            } catch (JobExecutionAlreadyRunningException | JobRestartException |
                     JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
                log.error("[Batch Admin] 배치 실행 실패 (파라미터 오류 또는 이미 실행 중): {}", e.getMessage());
            } catch (Exception e) {
                log.error("[Batch Admin] 배치 실행 중 예기치 않은 서버 오류 발생: {}", e.getMessage());
            }
        });

        return ResponseEntity.accepted().body("지수 시세 동기화 배치가 비동기로 시작되었습니다. 진행 상황은 로그를 확인하세요.");
    }

    private void validateDateFormat(String dateStr, String fieldName) {
        if (dateStr != null) {
            try {
                LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                log.warn("[Batch Admin] 잘못된 {} 형식 요청: {}", fieldName, dateStr);
                throw new BatchException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
    }
}
