package org.stockwellness.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.application.port.out.stock.StockPort;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/batch")
@RestController
public class BatchAdminController {

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;

    private final Job stockMasterSyncJob;
    private final Job stockPriceBatchJob;
    private final Job sectorEodJob;
    private final Job stockPricePrevCloseSyncJob;

    private final StockPort stockPort;

    /**
     * 특정 Job의 최근 실행 상태 조회
     */
    @GetMapping("/status/{jobName}")
    public List<String> getJobStatus(@PathVariable String jobName) {
        return jobExplorer.getJobInstances(jobName, 0, 5).stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .map(execution -> String.format("[%d] %s: %s (Start: %s, End: %s)",
                        execution.getId(),
                        execution.getJobInstance().getJobName(),
                        execution.getStatus(),
                        execution.getStartTime(),
                        execution.getEndTime()))
                .collect(Collectors.toList());
    }

    /**
     * 섹터 인사이트 배치 실행
     */
    @PostMapping("/sync-sector")
    public String runSectorSync(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("startDate", startDate)
                .addString("endDate", endDate)
                .toJobParameters();
        return launchJobAsync(sectorEodJob, params);
    }

    /**
     * 시세 수집 배치 실행 (전체 종목)
     */
    @PostMapping("/fetch-prices")
    public String runPriceFetch(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("startDate", startDate)
                .addString("endDate", endDate)
                .toJobParameters();
        return launchJobAsync(stockPriceBatchJob, params);
    }

    /**
     * 전일 종가 데이터 소급 보정 실행
     */
    @PostMapping("/sync-prev-close")
    public String runPrevCloseSync(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
//            @RequestParam(required = false) String targetTicker
    ) {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("startDate", startDate)
                .addString("endDate", endDate)
//                .addString("targetTicker", targetTicker)
                .toJobParameters();
        return launchJobAsync(stockPricePrevCloseSyncJob, params);
    }

    /**
     * 특정 종목 단건 즉시 동기화 (보정 배치 활용)
     */
    @PostMapping("/sync-stock/{ticker}")
    public String syncSingleStock(
            @PathVariable String ticker,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        if (!stockPort.existsByTicker(ticker)) {
            throw new IllegalArgumentException("존재하지 않는 종목입니다: " + ticker);
        }

        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("targetTicker", ticker)
                .addString("startDate", startDate)
                .addString("endDate", endDate)
                .toJobParameters();

        return launchJobAsync(stockPricePrevCloseSyncJob, params);
    }

    /**
     * 실행 중인 배치 중단
     */
    @PostMapping("/stop/{executionId}")
    public String stopJob(@PathVariable Long executionId) {
        try {
            boolean result = jobOperator.stop(executionId);
            return result ? "중단 요청 성공" : "중단 실패 (이미 종료되었을 수 있음)";
        } catch (Exception e) {
            return "오류 발생: " + e.getMessage();
        }
    }

    private String launchJobAsync(Job job, JobParameters params) {
        CompletableFuture.runAsync(() -> {
            try {
                jobLauncher.run(job, params);
            } catch (Exception e) {
                log.error("Job {} failed", job.getName(), e);
            }
        });
        return job.getName() + " 시작됨 (ExecutionId는 로그 확인 필요)";
    }
}
