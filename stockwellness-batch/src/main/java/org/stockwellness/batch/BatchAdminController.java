package org.stockwellness.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.batch.job.stock.master.MarketIndexSyncService;
import org.stockwellness.batch.job.stock.price.StockPriceSyncRequest;

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
    private final MarketIndexSyncService marketIndexSyncService;
    private final TaskExecutor kisBatchExecutor;

    /**
     * 업종/지수 마스터(idxcode.mst) 동기화
     */
    @PostMapping("/sync-indices")
    public String runIndicesSync() {
        try {
            marketIndexSyncService.syncIndices("idxcode.mst");
            return "업종 마스터 동기화 성공 (로그 확인 필요)";
        } catch (Exception e) {
            log.error("업종 마스터 동기화 실패", e);
            return "동기화 실패: " + e.getMessage();
        }
    }

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
     * 종목 마스터(KOSPI/KOSDAQ) 동기화 배치 실행
     */
    @PostMapping("/sync-master")
    public String runMasterSync() {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        return launchJobAsync(stockMasterSyncJob, params);
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
            @RequestBody(required = false) StockPriceSyncRequest request,
            @RequestParam(defaultValue = "false") boolean publishEvent
    ) {
        String startDate = null;
        String endDate = null;

        if (request != null) {
            startDate = request.getStartDate();
            endDate = request.getEndDate();

            if (startDate != null && startDate.compareTo("20220101") < 0) {
                log.info("Requested startDate {} is before 20220101. Adjusted to 20220101.", startDate);
                startDate = "20220101";
            }
        }

        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("startDate", startDate)
                .addString("endDate", endDate)
                .addString("publishEvent", String.valueOf(publishEvent))
                .toJobParameters();
        return launchJobAsync(stockPriceBatchJob, params);
    }

    /**
     * 시세 수집 배치 실행 (단건 종목)
     */
    @PostMapping("/fetch-prices/single")
    public String runSinglePriceFetch(
            @RequestBody StockPriceSyncRequest request,
            @RequestParam(defaultValue = "false") boolean publishEvent
    ) {
        if (request == null || !StringUtils.hasText(request.getTargetTicker())) {
            throw new IllegalArgumentException("targetTicker는 필수입니다.");
        }

        String startDate = request.getStartDate();
        if (startDate != null && startDate.compareTo("20220101") < 0) {
            startDate = "20220101";
        }

        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("targetTicker", request.getTargetTicker())
                .addString("startDate", startDate)
                .addString("endDate", request.getEndDate())
                .addString("publishEvent", String.valueOf(publishEvent))
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
    ) {
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("startDate", startDate)
                .addString("endDate", endDate)
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
        }, kisBatchExecutor);
        return job.getName() + " 시작됨 (ExecutionId는 로그 확인 필요)";
    }
}
