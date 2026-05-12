package org.stockwellness.adapter.in.web.batch;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.stockwellness.adapter.in.web.batch.dto.BatchExecutionResponse;
import org.stockwellness.application.port.in.batch.BatchControlUseCase;
import org.stockwellness.batch.support.exception.BatchException;
import org.stockwellness.global.common.response.ApiResponse;
import org.stockwellness.global.error.ErrorCode;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/batch")
@RequiredArgsConstructor
public class BenchmarkController {

    private final BatchControlUseCase batchControlUseCase;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 지수 시세 동기화 배치 수동 실행
     * @param startDate 수집 시작일 (yyyyMMdd, 선택 사항)
     * @param endDate 수집 종료일 (yyyyMMdd, 선택 사항)
     */
    @PostMapping("/benchmark-sync")
    public ResponseEntity<ApiResponse<BatchExecutionResponse>> syncBenchmarkPrice(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("[Batch Admin] 지수 시세 동기화 수동 실행 요청 수신 (시작일: {}, 종료일: {})", startDate, endDate);

        // 날짜 형식 유효성 검증
        validateDateFormat(startDate, "시작일");
        validateDateFormat(endDate, "종료일");
        BatchControlUseCase.BatchExecutionResult result = batchControlUseCase.launchAsync(new BatchControlUseCase.BatchLaunchCommand(
                BatchControlUseCase.BatchJobType.BENCHMARK_PRICE_SYNC,
                null,
                startDate,
                endDate,
                null,
                false
        ));

        BatchExecutionResponse response = new BatchExecutionResponse(
                result.executionId(),
                result.jobName(),
                result.statusUrl(),
                result.message()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
    }

    private void validateDateFormat(String dateStr, String fieldName) {
        if (dateStr != null) {
            try {
                LocalDate.parse(dateStr, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                log.warn("[Batch Admin] 잘못된 {} 형식 요청: {}", fieldName, dateStr);
                throw new BatchException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
    }
}
