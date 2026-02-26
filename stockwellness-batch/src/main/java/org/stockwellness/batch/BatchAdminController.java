package org.stockwellness.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.batch.job.stock.price.StockPriceProcessor;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

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

    private final StockPort stockPort;
    private final StockPriceProcessor stockPriceProcessor;

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
    public String runSectorSync() {
        return launchJobAsync(sectorEodJob, new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters());
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
     * 특정 종목 단건 즉시 동기화 (배치 로직 재사용)
     */
    @PostMapping("/sync-stock/{ticker}")
    public String syncSingleStock(@PathVariable String ticker) {
        Stock stock = stockPort.loadStockByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 종목입니다: " + ticker));

        CompletableFuture.runAsync(() -> {
            try {
                log.info("종목 {} 단건 동기화 시작", ticker);
                List<StockPrice> result = stockPriceProcessor.process(List.of(stock));
                // Note: Writer를 직접 호출하거나 별도 저장 로직이 필요할 수 있으나, 
                // 여기서는 Processor 로직 검증 및 결과 로깅 위주로 먼저 구현.
                log.info("종목 {} 동기화 완료: {}건 처리됨", ticker, (result != null ? result.size() : 0));
            } catch (Exception e) {
                log.error("Single stock sync failed for {}", ticker, e);
            }
        });
        return ticker + " 종목 동기화 요청 접수";
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
